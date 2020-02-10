package de.heoegbr.bgproxy.db;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BgReadingRepository {
    private static final String TAG = "REPOSITORY";

    private static BgReadingRepository INSTANCE;

    private SharedPreferences mPrefs;
    private BgReadingDao mBgReadingDao;
    private LiveData<List<BgReading>> mAllBgReadings;
    private Executor mExecutor = Executors.newSingleThreadExecutor();

    private BgReading latestInsertedReading = null;

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
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                // prime gap tracking
                List<BgReading> items = mBgReadingDao.getStaticReadings();
                if (items != null && !items.isEmpty()) {
                    latestInsertedReading = items.get(0);
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
        final List<BgReading> readingsToPush = new ArrayList<>();
        readingsToPush.add(bgReading);

        // check for gaps
        long diff = bgReading.date - latestInsertedReading.date;
        if (latestInsertedReading != null && diff > 3600000 /*=1h*/) {
            // clear database in case of big gaps (most probably new sensor was set)
            Log.d(TAG, "Kill Database (New Sensor?)");
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    mBgReadingDao.deleteAll();
                }
            });
        } else if (latestInsertedReading != null && diff > 300000 /*=5min*/) {
            // fill gaps with interpolation or zeros in case of small gaps

            // calculate no of missing readings
            int noOfMissingReadings = (int) Math.floor(diff / 290000) - 1;
            Log.d(TAG, noOfMissingReadings + " readings missing.");

            // calculate timestamps for missing readings
            long[] datesOfMissingReadings = new long[noOfMissingReadings];
            for (int i = 0; i < noOfMissingReadings; i++) {
                datesOfMissingReadings[i] = bgReading.date - (i + 1) * 300000;
            }

            // calculate values for missing readings
            double[] valuesOfMissingReadings = new double[noOfMissingReadings];
            if (noOfMissingReadings < 4 || !mPrefs.getBoolean("interpolation_en", false)) {
                // interpolation not enabled or gap too big --> fill with zeros
                valuesOfMissingReadings = interpolateBGs(
                        bgReading.value,
                        latestInsertedReading.value,
                        noOfMissingReadings);
            } else {
                // interpolation enabled
                for (int i = 0; i < noOfMissingReadings; i++) {
                    valuesOfMissingReadings[i] = 0.0;
                }
            }

            // build reading objects
            for (int i = 0; i < noOfMissingReadings; i++) {
                BgReading tmpReading = new BgReading();
                tmpReading.date = datesOfMissingReadings[i];
                tmpReading.value = valuesOfMissingReadings[i];
                Log.d(TAG,tmpReading.toString());
                readingsToPush.add(tmpReading);
            }
        }

        // add new value(s)
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                for (BgReading item : readingsToPush) {
                    mBgReadingDao.insert(item);
                }
                latestInsertedReading = bgReading;
            }
        });

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
        interpolation = y1 + x / 10 * (y2 - y1);

        Log.d(TAG, "Interpolated " + interpolation + " between " + y1 + " and " + y2);
        return interpolation;
    }
}
