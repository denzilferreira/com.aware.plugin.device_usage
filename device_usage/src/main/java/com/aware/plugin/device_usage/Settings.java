package com.aware.plugin.device_usage;

import com.aware.Aware;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Settings extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	
	/**
	 * State of this plugin
	 */
	public static final String STATUS_PLUGIN_DEVICE_USAGE = "status_plugin_device_usage";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		syncSettings();
	}
	
	private void syncSettings() {
		CheckBoxPreference check = (CheckBoxPreference) findPreference(STATUS_PLUGIN_DEVICE_USAGE);
		check.setChecked(Aware.getSetting(getApplicationContext(), STATUS_PLUGIN_DEVICE_USAGE).equals("true"));
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		syncSettings();
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference preference = (Preference) findPreference(key);
		if( preference.getKey().equals(STATUS_PLUGIN_DEVICE_USAGE) ) {
            Aware.setSetting(getApplicationContext(), key, sharedPreferences.getBoolean(key, false));

            if( sharedPreferences.getBoolean(key, false) ) {
				Aware.startPlugin(getApplicationContext(), getPackageName());
			} else {
				Aware.stopPlugin(getApplicationContext(), getPackageName());
			}
		}
	}
}
