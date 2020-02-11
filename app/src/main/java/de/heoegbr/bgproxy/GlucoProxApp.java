package de.heoegbr.bgproxy;

import android.Manifest;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import de.heoegbr.bgproxy.receiver.XdripDataReceiver;
import de.heoegbr.bgproxy.service.DontDieService;
import de.heoegbr.bgproxy.ui.PreferencesFragment;

public class GlucoProxApp extends Application {
    public static final String CHANNEL_ID = "de.heoegbr.bgproxy.notifications";
    private static final String TAG = "BGPRXY_APP";

    public static void registerBroadcastReceivers(Context context) {
        context.registerReceiver(
                new XdripDataReceiver(),
                new IntentFilter(XdripDataReceiver.XDRIP_ACTION_NEW_ESTIMATE));
//    } else if (Intents.NS_EMULATOR.equals(action)) {
//        SourceMM640gPlugin.getPlugin().handleNewData(intent);
//    } else if (Intents.GLIMP_BG.equals(action)) {
//        SourceGlimpPlugin.getPlugin().handleNewData(intent);
//    } else if (Intents.DEXCOM_BG.equals(action)) {
//        SourceDexcomPlugin.INSTANCE.handleNewData(intent);
//    } else if (Intents.POCTECH_BG.equals(action)) {
//        SourcePoctechPlugin.getPlugin().handleNewData(intent);
//    } else if (Intents.TOMATO_BG.equals(action)) {
//        SourceTomatoPlugin.getPlugin().handleNewData(intent);
//    } else if (Intents.EVERSENSE_BG.equals(action)) {
//        SourceEversensePlugin.getPlugin().handleNewData(intent);
        Log.d(TAG, "Receiver registered.");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OnCreate");

        // check if id is valid
        // TODO find clean solution for ID generation
        PreferencesFragment.getCleanBroadcastId(this);

        checkBleCapabilities(this);
        checkBtPermissions(this);
        registerBroadcastReceivers(getApplicationContext());

        createNotificationChannel();
        ContextCompat.startForegroundService(this,
                new Intent(getApplicationContext(), DontDieService.class));
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Example Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void checkBleCapabilities(Context context) {
        BluetoothManager manager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = manager.getAdapter();
        BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit().putBoolean("broadcast_en", false);
            return;
        }

        /*
         * Check for Bluetooth LE Support.  In production, our manifest entry will keep this
         * from installing on these devices, but this will allow test devices or other
         * sideloads to report whether or not the feature exists.
         */
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "No LE Support.", Toast.LENGTH_SHORT).show();
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit().putBoolean("broadcast_en", false);
            return;
        }

        /*
         * Check for advertising support. Not all devices are enabled to advertise
         * Bluetooth LE data.
         */
        if (!mBluetoothAdapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(context, "No Advertising Support.", Toast.LENGTH_SHORT).show();
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit().putBoolean("broadcast_en", false);
            return;
        }
    }

    private void checkBtPermissions(Context context) {
        // disable bt broadcast when no permission granted
        if (checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit().putBoolean("broadcast_en", false);
            return;
        }
    }
}
