package com.oscarprom.mediatest;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

/**
 * Created by Oscar Prom on 12/11/2014.
 */
public class MediaReceiver extends BroadcastReceiver {

    public MediaReceiver() {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String track = intent.getStringExtra("track");
        Toast.makeText(context, "Now playing " + track, Toast.LENGTH_SHORT).show();
    }
}
