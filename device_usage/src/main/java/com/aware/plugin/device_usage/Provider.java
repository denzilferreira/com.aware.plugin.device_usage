/**
@author: denzil
*/
package com.aware.plugin.device_usage;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Environment;
import android.provider.BaseColumns;
import android.util.Log;

import com.aware.*;
import com.aware.utils.DatabaseHelper;

public class Provider extends ContentProvider {

    /**
     * Authority of this content provider
     */
    public static String AUTHORITY = "com.aware.plugin.device_usage.provider.device_usage";
    
    /**
     * ContentProvider database version. Increment every time you modify the database structure
     */
    public static final int DATABASE_VERSION = 2;
    
    public static final class DeviceUsage_Data implements BaseColumns {
        private DeviceUsage_Data(){};
        
        /**
         * Your ContentProvider table content URI.<br/>
         * The last segment needs to match your database table name
         */
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/plugin_device_usage");
        
        /**
         * How your data collection is identified internally in Android (vnd.android.cursor.dir). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.aware.plugin.device_usage";
        
        /**
         * How each row is identified individually internally in Android (vnd.android.cursor.item). <br/>
         * It needs to be /vnd.aware.plugin.XXX where XXX is your plugin name (no spaces!).
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.aware.plugin.device_usage";
        
        public static final String _ID = "_id";
        public static final String TIMESTAMP = "timestamp";
        public static final String DEVICE_ID = "device_id";
        public static final String ELAPSED_DEVICE_ON = "elapsed_device_on";
        public static final String ELAPSED_DEVICE_OFF = "elapsed_device_off";
    }
    
    //ContentProvider query indexes
    private static final int DEVICE_USAGE = 1;
    private static final int DEVICE_USAGE_ID = 2;
    
    /**
     * Database stored in external folder: /AWARE/plugin_device_usage.db
     */
    public static final String DATABASE_NAME = "plugin_device_usage.db";
    
    /**
     * Database tables:<br/>
     * - plugin_phone_usage
     */
    public static final String[] DATABASE_TABLES = {"plugin_device_usage"};
    
    /**
     * Database table fields
     */
    public static final String[] TABLES_FIELDS = {
        DeviceUsage_Data._ID + " integer primary key autoincrement," +
        DeviceUsage_Data.TIMESTAMP + " real default 0," +
        DeviceUsage_Data.DEVICE_ID + " text default ''," +
        DeviceUsage_Data.ELAPSED_DEVICE_ON + " real default 0," +
        DeviceUsage_Data.ELAPSED_DEVICE_OFF + " real default 0," +
        "UNIQUE (" + DeviceUsage_Data.TIMESTAMP + "," + DeviceUsage_Data.DEVICE_ID +")"
    };
    
    private static UriMatcher sUriMatcher = null;
    private static HashMap<String, String> tableMap = null;
    private static DatabaseHelper databaseHelper = null;
    private static SQLiteDatabase database = null;
    
    private boolean initializeDB() {
        if (databaseHelper == null) {
            databaseHelper = new DatabaseHelper( getContext(), DATABASE_NAME, null, DATABASE_VERSION, DATABASE_TABLES, TABLES_FIELDS );
        }
        if( databaseHelper != null && ( database == null || ! database.isOpen() )) {
            database = databaseHelper.getWritableDatabase();
        }
        return( database != null && databaseHelper != null);
    }
    
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }

        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case DEVICE_USAGE:
            count = database.delete(DATABASE_TABLES[0], selection,
                    selectionArgs);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
        case DEVICE_USAGE:
            return DeviceUsage_Data.CONTENT_TYPE;
        case DEVICE_USAGE_ID:
            return DeviceUsage_Data.CONTENT_ITEM_TYPE;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues new_values) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        ContentValues values = (new_values != null) ? new ContentValues(new_values) : new ContentValues();

        switch (sUriMatcher.match(uri)) {
        case DEVICE_USAGE:
            long _id = database.insert(DATABASE_TABLES[0],
                    DeviceUsage_Data.DEVICE_ID, values);
            if (_id > 0) {
                Uri dataUri = ContentUris.withAppendedId(
                        DeviceUsage_Data.CONTENT_URI, _id);
                getContext().getContentResolver().notifyChange(dataUri, null);
                return dataUri;
            }
            throw new SQLException("Failed to insert row into " + uri);
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public boolean onCreate() {
    	
    	AUTHORITY = getContext().getPackageName() + ".provider.device_usage";
    	
    	sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0], DEVICE_USAGE); //URI for all records
        sUriMatcher.addURI(AUTHORITY, DATABASE_TABLES[0]+"/#", DEVICE_USAGE_ID); //URI for a single record
        
        tableMap = new HashMap<String, String>();
        tableMap.put(DeviceUsage_Data._ID, DeviceUsage_Data._ID);
        tableMap.put(DeviceUsage_Data.TIMESTAMP, DeviceUsage_Data.TIMESTAMP);
        tableMap.put(DeviceUsage_Data.DEVICE_ID, DeviceUsage_Data.DEVICE_ID);
        tableMap.put(DeviceUsage_Data.ELAPSED_DEVICE_ON, DeviceUsage_Data.ELAPSED_DEVICE_ON);
        tableMap.put(DeviceUsage_Data.ELAPSED_DEVICE_OFF, DeviceUsage_Data.ELAPSED_DEVICE_OFF);
    	
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return null;
        }

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sUriMatcher.match(uri)) {
        case DEVICE_USAGE:
            qb.setTables(DATABASE_TABLES[0]);
            qb.setProjectionMap(tableMap);
            break;
        default:
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        try {
            Cursor c = qb.query(database, projection, selection, selectionArgs,
                    null, null, sortOrder);
            c.setNotificationUri(getContext().getContentResolver(), uri);
            return c;
        } catch (IllegalStateException e) {
            if (Aware.DEBUG)
                Log.e(Aware.TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        if( ! initializeDB() ) {
            Log.w(AUTHORITY,"Database unavailable...");
            return 0;
        }
        
        int count = 0;
        switch (sUriMatcher.match(uri)) {
        case DEVICE_USAGE:
            count = database.update(DATABASE_TABLES[0], values, selection,
                    selectionArgs);
            break;
        default:
            database.close();
            throw new IllegalArgumentException("Unknown URI " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }
}
