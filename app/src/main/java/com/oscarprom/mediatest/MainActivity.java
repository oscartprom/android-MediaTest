package com.oscarprom.mediatest;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener {

  public BluetoothAdapter mBluetoothAdapter;
  public BluetoothSocket mBluetoothSocket;

  private static final int REQUEST_ENABLE_BT = 1;

  private BroadcastReceiver mReceiver;
  private IntentFilter mIntentFilter;

  private int mSingleAction;
  private int mDoubleAction;
  private int mLongAction;

  private Camera mCamera = null;
  private boolean mFlashlightOn = false;

  @Override
  protected void onCreate(Bundle savedInstanceState) {

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if (mBluetoothAdapter == null)
      Log.e("oscar", "no bluetooth");
    else {
      if (!mBluetoothAdapter.isEnabled()) {
        Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetoothIntent, REQUEST_ENABLE_BT);
      }
      else {
        ((TextView) findViewById(R.id.textViewBluetoothStatus)).setText("Bluetooth Status: On");
        selectBluetoothDevice();
      }
    }

    mReceiver = new MediaReceiver();
    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction("com.andrew.apollo.metachanged");
    registerReceiver(mReceiver, mIntentFilter);

    Spinner spinnerSingle = (Spinner) findViewById(R.id.spinnerSingle);
    Spinner spinnerDouble = (Spinner) findViewById(R.id.spinnerDouble);
    Spinner spinnerLong = (Spinner) findViewById(R.id.spinnerLong);
    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.options, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinnerSingle.setAdapter(adapter);
    spinnerSingle.setOnItemSelectedListener(this);
    spinnerDouble.setAdapter(adapter);
    spinnerDouble.setOnItemSelectedListener(this);
    spinnerLong.setAdapter(adapter);
    spinnerLong.setOnItemSelectedListener(this);
  }

  public void socketConnected(BluetoothSocket socket) {

    mBluetoothSocket = socket;
    ConnectedThread thread = new ConnectedThread(MainActivity.this);
    thread.start();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_ENABLE_BT) {
      if (resultCode == RESULT_OK) {
        ((TextView) MainActivity.this.findViewById(R.id.textViewBluetoothStatus)).setText("Bluetooth Status: On");
        selectBluetoothDevice();
      }
      else
        ((TextView) MainActivity.this.findViewById(R.id.textViewBluetoothStatus)).setText("Bluetooth Status: Off");
    }
  }

  public void selectBluetoothDevice() {

    ArrayAdapter<String> deviceArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);

    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    final ArrayList<BluetoothDevice> deviceList = new ArrayList<BluetoothDevice>();
    if (pairedDevices.size() > 0) {
      for (BluetoothDevice bd : pairedDevices) {
        deviceArrayAdapter.add(bd.getName());
        deviceList.add(bd);
      }
    }

    ListView list = new ListView(this);
    list.setAdapter(deviceArrayAdapter);

    final AlertDialog dialog = new AlertDialog.Builder(this)
      .setView(list)
      .setCancelable(false)
      .create();

    list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        BluetoothDevice device = deviceList.get(position);
        ConnectThread thread = new ConnectThread(MainActivity.this, device);
        thread.start();

        dialog.dismiss();
      }
    });

    dialog.show();
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    Log.d("oscar", "set new action");
    if (parent.getId() == R.id.spinnerSingle) {
      if (pos == 0)
        mSingleAction = Constants.ACTION_PLAYPAUSE;
      else if (pos == 1)
        mSingleAction = Constants.ACTION_NEXT;
      else if (pos == 2)
        mSingleAction = Constants.ACTION_PREV;
      else if (pos == 3)
        mSingleAction = Constants.ACTION_TOGGLEFLASHLIGHT;
    }
    else if (parent.getId() == R.id.spinnerDouble) {
      if (pos == 0)
        mDoubleAction = Constants.ACTION_PLAYPAUSE;
      else if (pos == 1)
        mDoubleAction = Constants.ACTION_NEXT;
      else if (pos == 2)
        mDoubleAction = Constants.ACTION_PREV;
      else if (pos == 3)
        mDoubleAction = Constants.ACTION_TOGGLEFLASHLIGHT;
    }
    else if (parent.getId() == R.id.spinnerLong) {
      if (pos == 0)
        mLongAction = Constants.ACTION_PLAYPAUSE;
      else if (pos == 1)
        mLongAction = Constants.ACTION_NEXT;
      else if (pos == 2)
        mLongAction = Constants.ACTION_PREV;
      else if (pos == 3)
        mLongAction = Constants.ACTION_TOGGLEFLASHLIGHT;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {}

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

  public void doAction(int pressType) {
    if (pressType == Constants.PRESS_SINGLE) {
      switch (mSingleAction) {
        case Constants.ACTION_NEXT:
          mediaNext();
          break;
        case Constants.ACTION_PREV:
          mediaPrevious();
          break;
        case Constants.ACTION_PLAYPAUSE:
          mediaPlayPause();
          break;
        case Constants.ACTION_TOGGLEFLASHLIGHT:
          toggleFlashlight();
          break;
      }
    }
    else if (pressType == Constants.PRESS_DOUBLE) {
      switch (mDoubleAction) {
        case Constants.ACTION_NEXT:
          mediaNext();
          break;
        case Constants.ACTION_PREV:
          mediaPrevious();
          break;
        case Constants.ACTION_PLAYPAUSE:
          mediaPlayPause();
          break;
        case Constants.ACTION_TOGGLEFLASHLIGHT:
          toggleFlashlight();
          break;
      }
    }
    else if (pressType == Constants.PRESS_LONG) {
      switch (mLongAction) {
        case Constants.ACTION_NEXT:
          mediaNext();
          break;
        case Constants.ACTION_PREV:
          mediaPrevious();
          break;
        case Constants.ACTION_PLAYPAUSE:
          mediaPlayPause();
          break;
        case Constants.ACTION_TOGGLEFLASHLIGHT:
          toggleFlashlight();
          break;
      }
    }
  }

  public void mediaPrevious() {
    sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PREVIOUS, false);
  }

  public void mediaPlayPause() {
    sendMediaIntent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, false);
  }

  public void mediaNext() {
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

  public void toggleFlashlight() {
    if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {

      mFlashlightOn = !mFlashlightOn;

      if (mCamera == null)
        mCamera = Camera.open();

      if (mFlashlightOn) {
        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(p);
        mCamera.startPreview();
      }
      else {
        Camera.Parameters p = mCamera.getParameters();
        p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(p);
        mCamera.stopPreview();
      }
    }
  }
}
