package de.heoegbr.bgproxy.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import de.heoegbr.bgproxy.db.BgReading;
import de.heoegbr.bgproxy.db.BgReadingRepository;

public class XdripDataReceiver extends BroadcastReceiver {
    public static final String XDRIP_ACTION_NEW_ESTIMATE = "com.eveningoutpost.dexdrip.BgEstimate";
    private static final String TAG = "XDRIP_DATA_RECEIVER";
    private static final String XDRIP_EXTRA_BG_ESTIMATE = "com.eveningoutpost.dexdrip.Extras.BgEstimate";
    private static final String XDRIP_EXTRA_BG_SLOPE_NAME = "com.eveningoutpost.dexdrip.Extras.BgSlopeName";
    private static final String XDRIP_EXTRA_TIMESTAMP = "com.eveningoutpost.dexdrip.Extras.Time";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "RECEIVED: " + intent);

        // check if intent is for me
        if (XDRIP_ACTION_NEW_ESTIMATE.equalsIgnoreCase(intent.getAction())) {
            // write bg to db
            handleIntentExtras(context, intent);
        }
    }


    public void handleIntentExtras(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;
        Log.d(TAG, "Received xDrip data: " + intent.getExtras());

        // create bg reading
        BgReading bgReading = new BgReading();

        bgReading.value = bundle.getDouble(XDRIP_EXTRA_BG_ESTIMATE);
        bgReading.direction = bundle.getString(XDRIP_EXTRA_BG_SLOPE_NAME);
        bgReading.date = bundle.getLong(XDRIP_EXTRA_TIMESTAMP);
        Log.d(TAG, "BG READING: " + bgReading.toString());

        // push to database
        BgReadingRepository.getRepository(context).insert(bgReading);
    }
}
