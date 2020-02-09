package de.heoegbr.bgproxy.ble;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public final class WorkScheduleHelper {
    private static final String TAG = "WORK_SCHEDULE_HELPER";
    private WorkScheduleHelper() {
    }

    public static void scheduleBtBroadcast(Context context) {
        if (PreferenceManager
                .getDefaultSharedPreferences(context)
                .getBoolean("broadcast_en", false)) {
            OneTimeWorkRequest oneTimeWorkRequest =
                    new OneTimeWorkRequest.Builder(BtBgAdvertiserWorker.class).build();
            WorkManager.getInstance(context).enqueue(oneTimeWorkRequest);
            Log.d(TAG, "BT broadcast scheduled");
        } else {
            Log.d(TAG, "BT broadcast disabled. Nothing scheduled.");
        }
    }
}
