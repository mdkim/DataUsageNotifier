package com.datausagenotifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ListView;

import com.datausagenotifier.model.TrafficStatsArrayAdapter;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver receiver;
    private TrafficStatsArrayAdapter statsArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView1);
        TrafficStatsArrayAdapter adapter = new TrafficStatsArrayAdapter(this, R.layout.traffic_stats, R.id.textView1);
        listView.setAdapter(adapter);
        this.statsArrayAdapter = adapter;

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, DataUsageMonitorService.class);
                Button button = (Button) view;
                if (DataUsageMonitorService.IS_STOPPED) {
                    startService(intent);
                    button.setText(R.string.stop_service);
                } else {
                    stopService(intent);
                    button.setText(R.string.start_service);
                }
            }
        });

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                setTextSSBfromNotification(intent);
            }
        };
        IntentFilter intentFilter = new IntentFilter("update");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setTextSSBfromNotification(intent);
    }

    private void setTextSSBfromNotification(Intent intent) {
        if (!intent.getAction().equals("update")) return;
        CharSequence ssb = intent.getCharSequenceExtra("com.datausagenotifier.extras.ssb");
        if (ssb == null) return;

        this.statsArrayAdapter.insert(ssb, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Button button = (Button) findViewById(R.id.button);
        if (DataUsageMonitorService.IS_STOPPED) {
            button.setText(R.string.start_service);
        } else {
            button.setText(R.string.stop_service);
        }

        // redundant if already called from onNewIntent, but this is simpler
        Intent intent = getIntent();
        setTextSSBfromNotification(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            try {
                unregisterReceiver(receiver);
            } catch (IllegalArgumentException e) {
                // not registered
            }
            receiver = null;
        }
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
        if (id == R.id.action_exit) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
