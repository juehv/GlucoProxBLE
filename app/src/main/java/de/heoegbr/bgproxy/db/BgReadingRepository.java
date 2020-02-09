package de.heoegbr.bgproxy.db;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BgReadingRepository {
    private static final String TAG = "REPOSITORY";

    private static BgReadingRepository INSTANCE;

    private BgReadingDao mBgReadingDao;
    private LiveData<List<BgReading>> mAllBgReadings;
    private Executor mExecutor = Executors.newSingleThreadExecutor();

    private long mostRecentDate = -1;

    public static BgReadingRepository getRepository(final Context context) {
        if (INSTANCE == null) {
            synchronized (BgReadingRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BgReadingRepository(context);
                }
            }
        }
        return INSTANCE;
    }

    private BgReadingRepository(final Context context) {
        BgReadingDatabase db = BgReadingDatabase.getDatabase(context);
        mBgReadingDao = db.bgReadingDao();
        mAllBgReadings = mBgReadingDao.getLiveReadings();


        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // prime gap tracking
                List<BgReading> items = mBgReadingDao.getStaticReadings();
                if (items != null && !items.isEmpty()) {
                    mostRecentDate = items.get(0).date;
                }
            }
        });
    }

    public LiveData<List<BgReading>> getAllBgReadings() {
        return mAllBgReadings;
    }

    public List<BgReading> getMostRecentBgReadings() {
        return mBgReadingDao.getStaticReadings();
    }

    public void insert(final BgReading bgReading) {
        // check for big gaps
        long diff = bgReading.date - mostRecentDate;
        if (mostRecentDate > -1 && diff > 3600000) {
            // clear database in case of big gaps (most probably new sensor was set)
            Log.d(TAG, "Kill Database (New Sensor?)");
            // delete all and insert new values
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mBgReadingDao.deleteAll();
                    mBgReadingDao.insert(bgReading);
                    mostRecentDate = bgReading.date;
                }
            });
        } else {
            final List<BgReading> readingsToPush = new ArrayList<>();
            readingsToPush.add(bgReading);

            // interpolateOneValue small gaps --> generate missing values
            if (mAllBgReadings.getValue() != null) {
                Log.d(TAG, "Check data for gaps before adding new value");
                List<BgReading> tmpReadings = new ArrayList<>();
                tmpReadings.addAll(mAllBgReadings.getValue());

                long currentWindowStart = bgReading.date + 160000; // make current bg center of the 5 min window
                long currentWindowEnd;
                BgReading lastReading = bgReading;
                for (int i = 0; i < tmpReadings.size(); i++) {
                    // calculate new windows
                    currentWindowStart = lastReading.date - 140000;
                    currentWindowEnd = currentWindowStart - 320000;

                    long tmpDate = tmpReadings.get(i).date;

                    if (tmpDate > currentWindowStart || tmpDate < currentWindowEnd) {
                        Log.d(TAG, tmpDate + " not in window " + currentWindowEnd
                                + " to " + currentWindowEnd);
                        // not in current window --> calculate gap and interpolate
                        int valuesToCreate = (int) Math.floor((lastReading.date - tmpDate) / 290000) - 1;
                        Log.d(TAG, "Found gap with " + valuesToCreate + " missing Values.");
                        if (valuesToCreate > 0 && valuesToCreate < 4) {
                            // interpolateOneValue values ony if gap is smaller or equal 15 min
                            double[] missingValues = interpolateBGs(lastReading.value,
                                    tmpReadings.get(i).value, valuesToCreate);
                            for (int j = 0; j < valuesToCreate; j++) {
                                BgReading readingToAdd = new BgReading();
                                readingToAdd.date = lastReading.date - (j + 1) * 300000;
                                readingToAdd.value = missingValues[j];
                                readingsToPush.add(readingToAdd);
                            }
                        }
                    }
                    lastReading = tmpReadings.get(i);
                }
            }

            // add new value(s)
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mostRecentDate = bgReading.date;
                    for (BgReading item : readingsToPush) {
                        mBgReadingDao.insert(item);
                    }
                }
            });
        }

    }

    /**
     * Basic linear interpolation of BGs with formula y= f(x1) + (x-x1)/(x2-x1) * (f(x2)-f(x1))
     * whereby we set x1 = 0 and x2 = 10 and pre-calculate missing values to fit the 5 min rythm
     *
     * @param y1
     * @param y2
     * @param noOfMissingValues
     * @return
     */
    private double[] interpolateBGs(double y1, double y2, int noOfMissingValues) {
        double[] missingValues = new double[noOfMissingValues];
        for (int i = 0; i < noOfMissingValues; i++) {
            missingValues[i] = interpolateOneValue(y1, y2, (i + 1) * 10 / (noOfMissingValues + 1));
        }

        return missingValues;
    }

    private double interpolateOneValue(double y1, double y2, double x) {
        double interpolation = 0;
        interpolation = Double.valueOf(Math.round(y1 + x / 10 * (y2 - y1)));

        Log.d(TAG, "Interpolated " + interpolation + " between " + y1 + " and " + y2);
        return interpolation;
    }
}
