package de.heoegbr.bgproxy.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import java.util.Random;

import de.heoegbr.bgproxy.BuildConfig;
import de.heoegbr.bgproxy.R;


public class PreferencesFragment extends PreferenceFragmentCompat {
    private static final String TAG = "PREF_FRAGMENT";
    private static final int REQUEST_BT = 42;

    public static int getCleanBroadcastId(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int id;
        try {
            id = Integer.parseInt(prefs.getString("broadcast_id", "0"));
        } catch (NumberFormatException ex) {
            Log.d(TAG, ex.toString());
            id = 0;
        }
        if (id <= 0 || id >= 65534) {
            // id not valid --> Random ID
            id = new Random().nextInt(65500) + 1;
            prefs.edit().putInt("broadcast_id", id).apply();
        }
        return id;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        // customize input elements
        EditTextPreference idPrefField = findPreference("broadcast_id");
        if (idPrefField != null) {
            idPrefField.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
//                            editText.setFilters(new InputFilter[]{new InputFilter() {
//                                @Override
//                                public CharSequence filter(CharSequence source, int start, int end,
//                                                           Spanned dest, int dstart, int dend) {
//                                    return null;
//                                }
//                            }});
                        }
                    }
            );

            Preference.SummaryProvider<EditTextPreference> sumProv = new Preference.SummaryProvider<EditTextPreference>() {
                @Override
                public CharSequence provideSummary(EditTextPreference preference) {
                    int id = getCleanBroadcastId(getContext());
                    return String.format("Current ID 0x%04X", id);
                }
            };

            idPrefField.setSummaryProvider(sumProv);
        }

        EditTextPreference pwPrefField = findPreference("broadcast_password");
        if (pwPrefField != null) {
            pwPrefField.setOnBindEditTextListener(
                    new EditTextPreference.OnBindEditTextListener() {
                        @Override
                        public void onBindEditText(@NonNull EditText editText) {
                            editText.setInputType(InputType.TYPE_CLASS_TEXT |
                                    InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        }
                    }
            );
        }

        Preference buildVersion = findPreference("version");
        if (buildVersion != null) {
            buildVersion.setSummary(BuildConfig.VERSION_NAME);
        }


        SwitchPreferenceCompat broadcastPrefSwitch = findPreference("broadcast_en");
        if (broadcastPrefSwitch != null) {
            // disable option if ble is not supported
            if (!getContext().getPackageManager()
                    .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                broadcastPrefSwitch.setEnabled(false);
                broadcastPrefSwitch.setSummary("No BLE support!");
            }

            // add listener to ask for permissions
            broadcastPrefSwitch.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof Boolean && ((Boolean) newValue).booleanValue()) {

                        Log.d(TAG, "BT Broadcast on");
                        // check for permission
                        Activity parentActivity = getActivity();
                        if (parentActivity != null && (
                                parentActivity.checkSelfPermission(Manifest.permission.BLUETOOTH)
                                != PackageManager.PERMISSION_GRANTED
                                || parentActivity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN)
                                != PackageManager.PERMISSION_GRANTED)) {
                            // ask for permission
                            if (shouldShowRequestPermissionRationale(Manifest.permission.BLUETOOTH_ADMIN)) {
                                Toast.makeText(parentActivity.getApplicationContext(),
                                        getString(R.string.bt_permission_rationale),
                                        Toast.LENGTH_LONG).show();
                            }
                            requestPermissions(new String[]{Manifest.permission.BLUETOOTH,
                                    Manifest.permission.BLUETOOTH_ADMIN}, REQUEST_BT);
                        }
                    }
                    return true;
                }
            });
        }

        // add listener to send feedback
        Preference feedback = findPreference("feedback");
        if (feedback != null) {
            feedback.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    // Open Github
                    String url = "https://github.com/juehv/GlucoProxBLE/issues";
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                    return true;
                }
            });
        }
    }
}
