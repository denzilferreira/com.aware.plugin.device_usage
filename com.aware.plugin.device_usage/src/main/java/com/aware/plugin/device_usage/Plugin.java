/**
 * @author: denzil
 */
package com.aware.plugin.device_usage;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.Screen;
import com.aware.providers.Screen_Provider.Screen_Data;
import com.aware.ui.PermissionsHandler;
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

    //private variables that hold the latest values to be shared whenever ACTION_AWARE_CURRENT_CONTEXT is broadcasted
    private static double elapsed_device_off;
    private static double elapsed_device_on;

    private static ContextProducer sContext;

    /**
     * BroadcastReceiver that will receiver screen ON events from AWARE
     */
    private static ScreenListener screenListener = new ScreenListener();
    public static class ScreenListener extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_ON)) {
                //start timer on
                elapsed_device_on = 0;

                //Query screen data for when was the last time the screen was off
                Cursor last_time_off = context.getContentResolver().query(Screen_Data.CONTENT_URI, null, Screen_Data.SCREEN_STATUS + " = " + Screen.STATUS_SCREEN_OFF, null, Screen_Data.TIMESTAMP + " DESC LIMIT 1");
                if (last_time_off != null && last_time_off.moveToFirst()) {
                    //Calculate how long has it been until now that the screen was off
                    elapsed_device_off = System.currentTimeMillis() - last_time_off.getDouble(last_time_off.getColumnIndex(Screen_Data.TIMESTAMP));
                }
                if (last_time_off != null && !last_time_off.isClosed()) last_time_off.close();
            }

            if (intent.getAction().equals(Screen.ACTION_AWARE_SCREEN_OFF)) {
                //start timer off
                elapsed_device_off = 0;

                //Query screen data for when was the last time the screen was on
                Cursor last_time_on = context.getContentResolver().query(Screen_Data.CONTENT_URI, null, Screen_Data.SCREEN_STATUS + " = " + Screen.STATUS_SCREEN_ON, null, Screen_Data.TIMESTAMP + " DESC LIMIT 1");
                if (last_time_on != null && last_time_on.moveToFirst()) {
                    //Calculate how long has it been until now that the screen was on
                    elapsed_device_on = System.currentTimeMillis() - last_time_on.getDouble(last_time_on.getColumnIndex(Screen_Data.TIMESTAMP));
                }
                if (last_time_on != null && !last_time_on.isClosed()) last_time_on.close();
            }

            //Share context
            if (Plugin.sContext != null)
                Plugin.sContext.onContext();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        TAG = "AWARE::Device Usage";

        //create a context filter
        IntentFilter filter = new IntentFilter();
        filter.addAction(Screen.ACTION_AWARE_SCREEN_ON);
        filter.addAction(Screen.ACTION_AWARE_SCREEN_OFF);

        //Ask Android to register our context receiver
        registerReceiver(screenListener, filter);

        //Shares this plugin's context to AWARE and applications
        sContext = new ContextProducer() {
            @Override
            public void onContext() {
                ContentValues context_data = new ContentValues();
                context_data.put(Provider.DeviceUsage_Data.TIMESTAMP, System.currentTimeMillis());
                context_data.put(Provider.DeviceUsage_Data.DEVICE_ID, Aware.getSetting(getApplicationContext(), Aware_Preferences.DEVICE_ID));
                context_data.put(Provider.DeviceUsage_Data.ELAPSED_DEVICE_OFF, elapsed_device_off);
                context_data.put(Provider.DeviceUsage_Data.ELAPSED_DEVICE_ON, elapsed_device_on);

                if (DEBUG) Log.d(TAG, context_data.toString());

                //insert data to table
                getContentResolver().insert(Provider.DeviceUsage_Data.CONTENT_URI, context_data);

                Intent sharedContext = new Intent(ACTION_AWARE_PLUGIN_DEVICE_USAGE);
                sharedContext.putExtra(EXTRA_ELAPSED_DEVICE_OFF, elapsed_device_off);
                sharedContext.putExtra(EXTRA_ELAPSED_DEVICE_ON, elapsed_device_on);
                sendBroadcast(sharedContext);
            }
        };

        CONTEXT_PRODUCER = sContext;

        DATABASE_TABLES = Provider.DATABASE_TABLES;
        TABLES_FIELDS = Provider.TABLES_FIELDS;
        CONTEXT_URIS = new Uri[]{Provider.DeviceUsage_Data.CONTENT_URI};
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean permissions_ok = true;
        for(String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {

            DEBUG = Aware.getSetting(this, Aware_Preferences.DEBUG_FLAG).equals("true");

            Aware.setSetting(this, Settings.STATUS_PLUGIN_DEVICE_USAGE, true);
            Aware.setSetting(this, Aware_Preferences.STATUS_SCREEN, true);
            Aware.startAWARE(this);

        } else {
            Intent requestPermissions = new Intent(this, PermissionsHandler.class);
            requestPermissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            requestPermissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(requestPermissions);
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (screenListener != null)
            unregisterReceiver(screenListener);

        Aware.setSetting(this, Aware_Preferences.STATUS_SCREEN, false);
        Aware.stopScreen(this);

        Aware.setSetting(this, Settings.STATUS_PLUGIN_DEVICE_USAGE, false);

        Aware.stopAWARE(this);
    }
}
