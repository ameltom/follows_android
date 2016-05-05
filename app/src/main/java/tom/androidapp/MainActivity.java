package tom.androidapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "MainActivity";
    private ArrayList<PersonData> peopleData = new ArrayList<>();
    private String name = null;
    private String deviceId = null;
    private String code = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (deviceId == null) {
            try {
                TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                deviceId = telephonyManager.getDeviceId();
            } catch (SecurityException e) {
                Log.d(TAG, "No READ_PHONE_STATE Permission");
                return;
            }
        }

        FloatingActionButton addBtn = (FloatingActionButton) findViewById(R.id.addBtn);
        if (addBtn != null) {
            addBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    addFollower();
                }
            });
        }

        FloatingActionButton showCodeBtn = (FloatingActionButton) findViewById(R.id.showCodeBtn);
        if (showCodeBtn != null) {
            showCodeBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Your Code is:");
                    builder.setMessage(MainActivity.this.code);
                    builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    });
                    builder.setIcon(android.R.drawable.ic_dialog_info);
                    builder.show();
                }
            });
        }

        FloatingActionButton refreshBtn = (FloatingActionButton) findViewById(R.id.refreshBtn);
        if (refreshBtn != null) {
            refreshBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updatePeopleData();
                }
            });
        }

        setName(new Runnable() {
            @Override
            public void run() {
                AlarmScheduler.schedule(MainActivity.this);
                updatePeopleData();
            }
        }, false);
    }

    private void setName(final Runnable afterNameSet, boolean force) {
        final SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        name = pref.getString("name", null);
        if (name == null || force) {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Enter Your Name");
            final EditText input = new EditText(MainActivity.this);
            input.setInputType(InputType.TYPE_CLASS_TEXT);
            builder.setView(input);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    MainActivity.this.name = input.getText().toString();
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putString("name", MainActivity.this.name).commit();
                    afterNameSet.run();
                }
            });
            builder.show();
        } else
            afterNameSet.run();
    }

    private void addFollower() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Add Follower");
        builder.setMessage("Enter Follower Code");

// Set up the input
        final EditText input = new EditText(MainActivity.this);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        builder.setView(input);

// Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RequestParams params = new RequestParams();
                params.put("follower_code", input.getText().toString());
                HttpClientManager.asyncPost(MainActivity.this, "v1/add_follower/" + deviceId, params, new AsyncHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                        updatePeopleData();
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        Log.d(TAG, "get_code failure");
                    }
                });
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setIcon(android.R.drawable.ic_input_add);

        builder.show();
    }

    private void updatePeopleData() {
        HttpClientManager.asyncPost(this, "v1/follows_latest/" + deviceId, new RequestParams(), new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "updatePeopleData success");
                try {
                    String responseString = new String(responseBody);
                    JSONObject responseJsonObj = new JSONObject(responseString);
                    MainActivity.this.code = responseJsonObj.getString("code");
                    JSONArray peopleDataJsonArr = responseJsonObj.getJSONArray("people_data");

                    MainActivity.this.peopleData.clear();

                    for (int i = 0; i < peopleDataJsonArr.length(); i += 1) {
                        JSONObject p = peopleDataJsonArr.getJSONObject(i);
                        MainActivity.this.peopleData.add(new PersonData(
                                p.getString("name"),
                                p.getInt("battery_level"),
                                p.getInt("battery_scale"),
                                p.getDouble("gps_latitude"),
                                p.getDouble("gps_longitude"),
                                p.getLong("updated_at")
                        ));
                    }

                } catch (Throwable t) {
                    Log.e(TAG, "Can't read response from server");
                }

                setList();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Log.d(TAG, "updatePeopleData failure");
            }
        });
    }

    private void setList() {
        final ListView listview = (ListView) findViewById(R.id.mainlist);
        final ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < peopleData.size(); ++i) {
            list.add(peopleData.get(i).toViewString());
        }
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, list);
        listview.setAdapter(adapter);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, final View view, int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                final PersonData personData = peopleData.get(position);
                view.animate().setDuration(500).alpha(0.4f)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                view.setAlpha(1);
                                if (personData.GPSLatitude != 0 || personData.GPSLongitude != 0) {
                                    String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f (%s)", personData.GPSLatitude, personData.GPSLongitude, personData.GPSLatitude, personData.GPSLongitude, personData.name);
                                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                                    intent.setClassName("com.google.android.apps.maps", "com.google.android.maps.MapsActivity");
                                    try {
                                        MainActivity.this.startActivity(intent);
                                    } catch (Throwable throwable) {
                                        Log.d(TAG, "No Google Maps");
                                    }
                                } else {
                                    Log.d(TAG, "No GPS Data");
                                }
                            }
                        });
            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class StableArrayAdapter extends ArrayAdapter<String> {

        HashMap<String, Integer> mIdMap = new HashMap<>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
