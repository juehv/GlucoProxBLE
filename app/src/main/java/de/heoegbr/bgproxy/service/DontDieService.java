package de.heoegbr.bgproxy.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;
import androidx.lifecycle.Observer;

import java.util.List;

import de.heoegbr.bgproxy.GlucoProxApp;
import de.heoegbr.bgproxy.R;
import de.heoegbr.bgproxy.ble.WorkScheduleHelper;
import de.heoegbr.bgproxy.db.BgReading;
import de.heoegbr.bgproxy.db.BgReadingRepository;
import de.heoegbr.bgproxy.ui.MainActivity;

public class DontDieService extends LifecycleService {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, 0);

        // TODO make notification useful (last broadcast, last received bg reading ...)
        Notification notification = new NotificationCompat.Builder(this, GlucoProxApp.CHANNEL_ID)
                .setContentTitle("GlucoProxBLE")
                .setContentText("Service is running.")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_service)
                .build();

        startForeground(1, notification);

        BgReadingRepository.getRepository(getApplicationContext())
                .getLiveReadings().observe(this, new Observer<List<BgReading>>() {
            @Override
            public void onChanged(List<BgReading> bgReadings) {

                WorkScheduleHelper.scheduleBtBroadcast(getApplicationContext());
            }
        });

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }
}
