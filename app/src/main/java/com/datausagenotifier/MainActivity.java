package com.datausagenotifier;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import com.datausagenotifier.model.TrafficStatsArrayAdapter;
import com.datausagenotifier.model.TrafficStatsArrayItem;
import com.datausagenotifier.util.Const;

import org.json.JSONException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final boolean IS_TEST_DATA = false;
    public static final int POLLING_INTERVAL_MS = 8000;
    public static final int MAX_AUTO_UPDATES = 20;

    private BroadcastReceiver receiver;
    private TrafficStatsArrayAdapter statsArrayAdapter;
    private AlertDialog alertDialog;

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
                refreshButtonLabel();
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Const.ACTION_UPDATE);
        intentFilter.addAction(Const.ACTION_NONE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);

        AlertDialog alertDialog = new AlertDialog.Builder(this).create(); // THEME
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        this.alertDialog = alertDialog;
    }

    // for broadcast receiving, see onReceive() above
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setTextSSBfromNotification(intent);
    }

    private void setTextSSBfromNotification(Intent intent) {
        if (!intent.getAction().equals(Const.ACTION_UPDATE)) return;

        CharSequence ssb = intent.getCharSequenceExtra(Const.EXTRAS_SSB);
        if (ssb == null) return;

        Boolean isFirstPass = intent.getBooleanExtra(Const.EXTRAS_ISFIRSTPASS, false);

        List<String> packageNames = intent.getStringArrayListExtra(Const.EXTRAS_PACKAGE_NAMES);

        TrafficStatsArrayItem item = new TrafficStatsArrayItem(ssb, packageNames, isFirstPass);
        this.statsArrayAdapter.insert(item, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshButtonLabel();

        // redundant if already called from onNewIntent, but this is simpler
        Intent intent = getIntent();
        setTextSSBfromNotification(intent);
    }

    private void refreshButtonLabel() {
        Button button = (Button) findViewById(R.id.button);
        if (DataUsageMonitorService.IS_STOPPED) {
            button.setText(R.string.start_service);
        } else {
            button.setText(R.string.stop_service);
        }
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
        } else if (id == R.id.action_clear) {
            this.statsArrayAdapter.clear();
            this.clearCache();
            return true;
        } else if (id == R.id.action_export) {
            int status = this.statsArrayAdapter.exportLogs();
            if (status < 0) {
                this.alertDialog.setTitle("Error exporting logs");
                this.alertDialog.setMessage("Status code = " + status);
                this.alertDialog.show();
            } else if (status > 0) {
                this.alertDialog.setTitle("Exported logs");
                this.alertDialog.setMessage("Log file exported to Download folder");
                this.alertDialog.show();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // store statsArrayAdapter to cache
        File file = new File(getCacheDir(), Const.FILE_CACHE_STATSARRAYADAPTER);
        try (
                FileWriter fw = new FileWriter(file)
        ) {
            this.statsArrayAdapter.serialize(fw);
        } catch (IOException | JSONException e) {
            new RuntimeException(e);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (this.statsArrayAdapter.getCount() > 0) return;

        // read statsArrayAdapter from cache
        File file = new File(getCacheDir(), Const.FILE_CACHE_STATSARRAYADAPTER);
        if (!file.exists()) return;
        try (
                FileReader reader = new FileReader(file)
        ) {
            this.statsArrayAdapter.deserialize(reader);
        } catch (IOException e) {
            new RuntimeException(e);
        }
    }

    private void clearCache() {
        File file = new File(getCacheDir(), Const.FILE_CACHE_STATSARRAYADAPTER);
        FileWriter fw;
        try {
            fw = new FileWriter(file);
            fw.close();
        } catch (IOException e) {
            new RuntimeException(e);
        }
    }
}
