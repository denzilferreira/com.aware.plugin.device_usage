/**
 * @author: denzil
 */
package com.aware.plugin.device_usage;

import android.accounts.Account;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SyncRequest;
import android.os.Bundle;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Screen;
import com.aware.utils.Aware_Plugin;

public class Plugin extends Aware_Plugin {

    /**
     * Broadcasted event: the user has turned on his phone
     */
    public static final String ACTION_AWARE_PLUGIN_DEVICE_USAGE = "ACTION_AWARE_PLUGIN_DEVICE_USAGE";

    /**
     * Extra (double): how long was the phone OFF until the user turned it ON
     */
    public static final String EXTRA_ELAPSED_DEVICE_OFF = "elapsed_device_off";

    /**
     * Extra (double): how long was the phone ON until the user turned it OFF
     */
    public static final String EXTRA_ELAPSED_DEVICE_ON = "elapsed_device_on";

    private static long elapsed_device_off = 0;
    private static long elapsed_device_on = 0;
    private static long last_off = 0;
    private static long last_on = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        AUTHORITY = Provider.getAuthority(this);

        TAG = "AWARE::Device Usage";

        //Shares this plugin's context to AWARE and applications
        CONTEXT_PRODUCER = new ContextProducer() {
            @Override
            public void onContext() {
                ContentValues context_data = new ContentValues();
                context_data.put(Provider.DeviceUsage_Data.TIMESTAMP, System.currentTimeMillis());
                context_data.put(Provider.DeviceUsage_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                context_data.put(Provider.DeviceUsage_Data.ELAPSED_DEVICE_OFF, elapsed_device_off);
                context_data.put(Provider.DeviceUsage_Data.ELAPSED_DEVICE_ON, elapsed_device_on);

                if (DEBUG) Log.d(TAG, context_data.toString());

                //insert data to device usage table
                getContentResolver().insert(Provider.DeviceUsage_Data.CONTENT_URI, context_data);

                Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_DEVICE_USAGE);
                sharedContext.putExtra(EXTRA_ELAPSED_DEVICE_OFF, elapsed_device_off);
                sharedContext.putExtra(EXTRA_ELAPSED_DEVICE_ON, elapsed_device_on);
                sendBroadcast(sharedContext);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        if (PERMISSIONS_OK) {
            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_DEVICE_USAGE).length() == 0) {
                Aware.setSetting(getApplicationContext(), Settings.STATUS_PLUGIN_DEVICE_USAGE, true);
            } else {
                if (Aware.getSetting(getApplicationContext(), Settings.STATUS_PLUGIN_DEVICE_USAGE).equalsIgnoreCase("false")) {
                    Aware.stopPlugin(getApplicationContext(), getPackageName());
                    return START_STICKY;
                }
            }

            Aware.startScreen(this);
            Screen.setSensorObserver(new Screen.AWARESensorObserver() {
                @Override
                public void onScreenOn() {
                    //start timer on
                    elapsed_device_on = 0;
                }

                @Override
                public void onScreenOff() {
                    //start timer off
                    elapsed_device_off = 0;
                }

                @Override
                public void onScreenLocked() {
                    //locked, phone no longer allowed to use
                    if (last_on > 0) {
                        elapsed_device_off = 0;
                        elapsed_device_on = System.currentTimeMillis() - last_on;
                        CONTEXT_PRODUCER.onContext();
                    }
                    last_on = System.currentTimeMillis();
                }

                @Override
                public void onScreenUnlocked() {
                    //unlocked, phone ready to use
                    if (last_off > 0) {
                        elapsed_device_on = 0;
                        elapsed_device_off = System.currentTimeMillis() - last_off;
                        CONTEXT_PRODUCER.onContext();
                    }
                    last_off = System.currentTimeMillis();
                }
            });

            if (Aware.isStudy(this)) {
                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Provider.getAuthority(getApplicationContext());
                long frequency = Long.parseLong(Aware.getSetting(this, Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;

                ContentResolver.setIsSyncable(aware_account, authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, authority, true);
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Aware.setSetting(this, Settings.STATUS_PLUGIN_DEVICE_USAGE, false);
        Aware.stopScreen(this);

        ContentResolver.setSyncAutomatically(Aware.getAWAREAccount(this), Provider.getAuthority(this), false);
        ContentResolver.removePeriodicSync(
                Aware.getAWAREAccount(this),
                Provider.getAuthority(this),
                Bundle.EMPTY
        );

    }
}
