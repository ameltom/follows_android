package tom.androidapp;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.util.Log;

/**
 * Created by tomamel on 25/04/16.
 */
public class BootBroadcastReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = "BootBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        // Launch the specified service when this message is received
//        Intent startServiceIntent = new Intent(context, RepoerterService.class);
//        startWakefulService(context, startServiceIntent);

        AlarmScheduler.schedule(context);
    }
}
