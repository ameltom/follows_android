package tom.androidapp;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Vibrator;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

/**
 * Created by tomamel on 25/04/16.
 */
public class RepoerterService extends IntentService {
    private static String TAG = "RepoerterService";
    private static SyncHttpClient client = new SyncHttpClient();

    // Must create a default constructor
    public RepoerterService() {
        // Used to name the worker thread, important only for debugging.
        super("reporter-service");
    }

    @Override
    public void onCreate() {
        super.onCreate(); // if you override onCreate(), make sure to call super().
        // If a Context object is needed, call getApplicationContext() here.
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "onHandleIntent");
        // This describes what will happen when service is triggered

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        String name = pref.getString("name", null);

        if(name == null){
            Log.d(TAG, "name is null");
            return;
        }

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        Location location = getLastBestLocation();
        double gpsLatitude = 0;
        double gpsLongitude = 0;
        if (location != null) {
            gpsLatitude = location.getLatitude();
            gpsLongitude = location.getLongitude();
        }


        String deviceId = null;
        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            deviceId = telephonyManager.getDeviceId();
        } catch (SecurityException e) {
            Log.d(TAG, "No READ_PHONE_STATE Permission");
            return;
        }

//        try {
//            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
//            v.vibrate(50);
//        } catch (SecurityException e) {
//            Log.d(TAG, "No Vibrate Permission");
//        }

        Log.d(TAG, "name: " + name + ", deviceId: " + deviceId + ", batteryLevel: " + Integer.toString(batteryLevel) + ", batteryScale: " + Integer.toString(batteryScale) + ", gpsLatitude: " + Double.toString(gpsLatitude) + ", gpsLongitude: " + Double.toString(gpsLongitude));

        RequestParams params = new RequestParams();
        params.put("battery_level", batteryLevel);
        params.put("battery_scale", batteryScale);
        params.put("gps_latitude", gpsLatitude);
        params.put("gps_longitude", gpsLongitude);

        HttpClientManager.syncPost(this, "v1/report/" + deviceId, params);

        // Release the wake lock provided by the WakefulBroadcastReceiver.
        WakefulBroadcastReceiver.completeWakefulIntent(intent);
    }

    private Location getLastBestLocation() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location locationGPS;
        Location locationNet;
        try {
            locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } catch (SecurityException e) {
            Log.d(TAG, "No GPS Permission");
            return null;
        }

        long GPSLocationTime = 0;
        if (null != locationGPS) {
            GPSLocationTime = locationGPS.getTime();
        }

        long NetLocationTime = 0;

        if (null != locationNet) {
            NetLocationTime = locationNet.getTime();
        }

        if (0 < GPSLocationTime - NetLocationTime) {
            return locationGPS;
        } else {
            return locationNet;
        }
    }
}
