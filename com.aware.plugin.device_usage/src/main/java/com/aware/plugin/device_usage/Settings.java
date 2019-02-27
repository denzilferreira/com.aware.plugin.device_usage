package com.aware.plugin.device_usage;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.aware.Aware;
import com.aware.ui.AppCompatPreferenceActivity;

public class Settings extends AppCompatPreferenceActivity implements OnSharedPreferenceChangeListener {

    /**
     * State of this plugin
     */
    public static final String STATUS_PLUGIN_DEVICE_USAGE = "status_plugin_device_usage";

    private static CheckBoxPreference check;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_device_usage);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        check = (CheckBoxPreference) findPreference(STATUS_PLUGIN_DEVICE_USAGE);
        if (Aware.getSetting(this, STATUS_PLUGIN_DEVICE_USAGE).length() == 0) {
            Aware.setSetting(this, STATUS_PLUGIN_DEVICE_USAGE, true);
        }
        check.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_DEVICE_USAGE).equals("true"));
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Preference preference = (Preference) findPreference(key);
        if (preference.getKey().equals(STATUS_PLUGIN_DEVICE_USAGE)) {
            Aware.setSetting(this, key, sharedPreferences.getBoolean(key, false));
            check.setChecked(sharedPreferences.getBoolean(key, false));
        }
        if (Aware.getSetting(this, STATUS_PLUGIN_DEVICE_USAGE).equals("true")) {
            Aware.startPlugin(getApplicationContext(), "com.aware.plugin.device_usage");
        } else {
            Aware.stopPlugin(getApplicationContext(), "com.aware.plugin.device_usage");
        }
    }
}
