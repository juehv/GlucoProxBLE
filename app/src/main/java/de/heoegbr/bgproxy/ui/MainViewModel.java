package de.heoegbr.bgproxy.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import de.heoegbr.bgproxy.db.BgReading;
import de.heoegbr.bgproxy.db.BgReadingRepository;

public class MainViewModel extends AndroidViewModel {
    private BgReadingRepository mRepository;
    private LiveData<List<BgReading>> mAllReadings;

    public MainViewModel(@NonNull Application application) {
        super(application);
        mRepository = BgReadingRepository.getRepository(application.getApplicationContext());
        mAllReadings = mRepository.getLiveReadings();
    }

    public LiveData<List<BgReading>> getLiveReadings() {
        return mAllReadings;
    }
}
