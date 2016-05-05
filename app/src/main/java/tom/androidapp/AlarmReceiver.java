package tom.androidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by tomamel on 25/04/16.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "tom.androidapp.alarm";

    // Triggered by the Alarm periodically (starts the service to run task)
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        Intent i = new Intent(context, RepoerterService.class);
//        i.putExtra("foo", "bar");
        context.startService(i);
    }
}
