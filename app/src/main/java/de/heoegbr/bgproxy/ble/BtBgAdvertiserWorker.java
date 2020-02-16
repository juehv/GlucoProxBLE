package de.heoegbr.bgproxy.ble;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import de.heoegbr.bgproxy.db.BgReading;
import de.heoegbr.bgproxy.db.BgReadingRepository;
import de.heoegbr.bgproxy.ui.PreferencesFragment;

public class BtBgAdvertiserWorker extends Worker {

    private static final ParcelUuid GENERIC_SERVICE = ParcelUuid.fromString("00001801-0000-1000-8000-00805f9b34fb");
    //private static final ParcelUuid CGM_SERVICE = ParcelUuid.fromString("0000181F-0000-1000-8000-00805f9b34fb");
    //public static final ParcelUuid GLUCOSE_SERVICE = ParcelUuid.fromString("00001808-0000-1000-8000-00805f9b34fb");
    //public static final ParcelUuid BATTERY_SERVICE = ParcelUuid.fromString("0000180F-0000-1000-8000-00805f9b34fb");
    //public static final ParcelUuid DEVICE_INFORAMTION_SERVICE = ParcelUuid.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    private static final String TAG = "BT_WORKER";
    private final Context context;
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: " + errorCode);
        }
    };


    public BtBgAdvertiserWorker(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        // get data
        List<BgReading> readings = BgReadingRepository.getRepository(context)
                .getStaticReadings();

        // FIXME Debug code
//        if (readings != null) {
//            for (BgReading item : readings) {
//                Log.d(TAG, item.toString());
//            }
//        } else {
//            Log.d(TAG, "NO READINGS!!!");
//        }

        if (readings == null || readings.isEmpty()) return Result.failure();

        // calculate time offset to now
        long offset = (new Date().getTime()) - readings.get(0).date;
        offset = Math.abs(Math.round(offset / 60000)); // in minutes
        Log.d(TAG, "Time Offset:"+offset);
        if (offset > 254){
            Log.d(TAG, "Time offset too big. No advertisement!");
            return Result.failure();
        }

        // disable bt broadcast when no permission granted
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED) {
            PreferenceManager
                    .getDefaultSharedPreferences(context)
                    .edit().putBoolean("broadcast_en", false).apply();
            return Result.failure();
        }

        // prepare BLE
        BluetoothManager manager = (BluetoothManager) context
                .getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter mBluetoothAdapter = manager.getAdapter();

        /*
         * We need to enforce that Bluetooth is first enabled, and take the
         * user to settings to enable it if they have not done so.
         */
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            //Bluetooth is disabled
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            context.startActivity(enableBtIntent);
            return Result.failure();
        }

        BluetoothLeAdvertiser mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();

        // prapare BLE data and config
        if (mBluetoothLeAdvertiser == null) return Result.failure();
        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
                .setConnectable(false)
                .setTimeout(60000) // 1m
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
                .build();


        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(GENERIC_SERVICE)
                .addServiceData(GENERIC_SERVICE, buildPacket(context, readings, (byte) offset))
                .build();


//        AdvertiseData data2 = new AdvertiseData.Builder()
//                .setIncludeDeviceName(false)
//                .setIncludeTxPowerLevel(false)
//                .addServiceData(BATTERY_SERVICE, new byte[]{0x03})
//                .build();

        // start advertising
        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);

        Log.d(TAG, "Worker finished");
        return Result.success();
    }

    private byte[] buildPacket(Context context, List<BgReading> readings, byte timeOffsetInMinutes) {
        // get static data
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        // TODO find clean solution for ID management
        int broadcastId = PreferencesFragment.getCleanBroadcastId(context);
        String plainPassword = prefs.getString("broadcast_password", "");

        if (readings != null) {
            // estimate if values are in mmol/l
            boolean isMmol = true;
            for (BgReading item : readings) {
                if (item.value > 25.0) {
                    // should be a mg/dl
                    isMmol = false;
                    break;
                }
            }

            // build payload
            byte[] bqValuePayload = new byte[16];
            // prepare bg payload
            int entryCount = 0;
            while (entryCount < bqValuePayload.length) {
                if (readings.size() > entryCount) {
                    bqValuePayload[entryCount] = compressBgValue(readings.get(entryCount).value, isMmol);
                } else {
                    // if less than 16 values available fill with 0
                    bqValuePayload[entryCount] = 0x00;
                }
                entryCount++;
            }

            // encrypt values if password is set
            if (plainPassword != null && !plainPassword.isEmpty()) {
                Log.d(TAG, "AES Encryption");
                if (bqValuePayload.length != 14){
                    // make array to the correct size to fit into one encryption block
                    bqValuePayload = Arrays.copyOf(bqValuePayload, 14);
                }
                bqValuePayload = AesEncryptionHelper.encrypt(plainPassword, bqValuePayload);
            }

            // build package
            byte[] packageToSend = new byte[bqValuePayload.length + 3];
            // add id
            packageToSend[0] = (byte) (broadcastId & 0xFF);
            packageToSend[1] = (byte) ((broadcastId >> 8) & 0xFF);
            // add time offset
            packageToSend[2] = timeOffsetInMinutes;
            // add payload
            for (int i = 0; i < bqValuePayload.length; i++) {
                packageToSend[i + 3] = bqValuePayload[i];
            }

            return packageToSend;
        }
        return new byte[]{};
    }

    /**
     * Compresses a mg/dl value to one byte by dynamically reducing resolution for high values
     * 0 -> error
     * 1 -> low
     * 2 -> high
     * 3-152 -> normal span for 30-180 mg/dl
     * 153-207 -> low compressed span for 181-290 mg/dl
     * 208-254 -> high compressed span for 291-428 mg/dl
     *
     * @param bgValue uncompressed bg value
     * @return compressed bg value
     */
    private byte compressBgValue(double bgValue, boolean isMmol) {
        if (isMmol) {
            // convert to mg/dl
            bgValue = bgValue * 18;
        }

        // compress
        long tmpValue = Math.round(bgValue);
        if (tmpValue <= 0) {
            return (byte) 0;
        } else if (tmpValue < 30) {
            // error low
            return (byte) 1;
        } else if (tmpValue < 181) {
            // normal resolution
            return (byte) (tmpValue - 27);
        } else if (tmpValue < 291) {
            // half resolution
            return (byte) (153 + Math.round((tmpValue - 180) / 2));
        } else if (tmpValue < 429) {
            // 1/3 resolution
            return (byte) (208 + Math.round((tmpValue - 290) / 3));
        } else if (tmpValue >= 400) {
            // error high
            return (byte) 2;
        }

        return (byte) 0;
    }
}
