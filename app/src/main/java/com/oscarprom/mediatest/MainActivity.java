package com.oscarprom.mediatest;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class MainActivity extends ActionBarActivity {

    private BroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("oscar", "onCreate(): init and register");
        mReceiver = new MediaReceiver();
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction("com.andrew.apollo.metachanged");
        registerReceiver(mReceiver, mIntentFilter);
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

    @Override
    public void onPause() {
        super.onPause();
        Log.d("oscar", "onPause(): unregister");
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("oscar", "onResume(): re-register");
        registerReceiver(mReceiver, mIntentFilter);
    }

    public void mediaPrevious(View v) {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, false);
    }

    public void mediaPlayPause(View v) {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
    }

    public void mediaNext(View v) {
        sendMediaIntent(KeyEvent.KEYCODE_MEDIA_NEXT, false);
    }

    public void sendMediaIntent(final int i, boolean twice) {

        Log.d("oscar", "sending media intent");

        long time = SystemClock.uptimeMillis();

        Intent downIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent downEvent = new KeyEvent(time, time, KeyEvent.ACTION_DOWN, i, 0);
        downIntent.putExtra(Intent.EXTRA_KEY_EVENT, downEvent);
        sendOrderedBroadcast(downIntent, null);

        Intent upIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        KeyEvent upEvent = new KeyEvent(time, time, KeyEvent.ACTION_UP, i, 0);
        upIntent.putExtra(Intent.EXTRA_KEY_EVENT, upEvent);
        sendOrderedBroadcast(upIntent, null);

        if (twice) {
            new Handler().postDelayed(new Runnable() {
                public void run() {
                    sendMediaIntent(i, false);
                }
            }, 250);
        }
    }
}
