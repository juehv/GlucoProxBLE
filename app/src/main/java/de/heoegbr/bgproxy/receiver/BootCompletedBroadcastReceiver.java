package de.heoegbr.bgproxy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import de.heoegbr.bgproxy.R;

public class BootCompletedBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "BOOT_COMPLETE_RECEIVER";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Try to register BroadcastReceiver...");
        Toast.makeText(context, R.string.app_name + " started by boot completed intent", Toast.LENGTH_LONG).show();
        //FIXME does this work? Is the App object created (which registers the broadcast)?
    }
}
