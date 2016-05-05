package tom.androidapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.SyncHttpClient;

import cz.msebera.android.httpclient.Header;

/**
 * Created by tomamel on 28/04/16.
 */
public class HttpClientManager {
    private static String TAG = "HttpClientManager";
    private static String BASE_URL = "http://10.0.0.13:3000/";
    private static SyncHttpClient syncClient = new SyncHttpClient();
    private static AsyncHttpClient asyncClient = new AsyncHttpClient();
    private static AsyncHttpResponseHandler defaultResponse = new AsyncHttpResponseHandler() {
        @Override
        public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
            Log.d(TAG, "success");
        }

        @Override
        public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
            Log.d(TAG, "failure");
        }
    };

    public static void syncPost(Context context, String action, RequestParams params) {
        addDefaultParams(context, params);
        try {
            syncClient.post(BASE_URL + action, params, defaultResponse);
        } catch (Throwable t) {
            Log.d(TAG, "Internet Error");
        }
    }

    public static void asyncPost(Context context, String action, RequestParams params, final AsyncHttpResponseHandler responseHandler) {
        addDefaultParams(context, params);
        try {
            final ProgressDialog progressDialog = ProgressDialog.show(context, "Loading", "Please wait...", true);
            asyncClient.post(BASE_URL + action, params, new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    responseHandler.onSuccess(statusCode, headers, responseBody);
                    progressDialog.dismiss();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    responseHandler.onFailure(statusCode, headers, responseBody, error);
                    progressDialog.dismiss();
                }
            });
        } catch (Throwable t) {
            Log.d(TAG, "Internet Error");
        }
    }

    private static void addDefaultParams(Context context, RequestParams params) {
        SharedPreferences pref = context.getApplicationContext().getSharedPreferences("MyPref", context.MODE_PRIVATE);
        String name = pref.getString("name", null);
        params.put("name", name);
    }
}
