package com.oscarprom.mediatest;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class MainActivity extends ActionBarActivity implements AdapterView.OnItemSelectedListener {

  public BluetoothAdapter mBluetoothAdapter;
  public BluetoothSocket mBluetoothSocket;

  private static final int REQUEST_ENABLE_BT = 1000;
  private static final int REQUEST_CONTACT = 1001;

  private BroadcastReceiver mReceiver;
  private IntentFilter mIntentFilter;

  private HashMap<Integer, Integer> mActions;
  private HashMap<Integer, TextView> mDetailsTextViews;
  private int mActionPendingResult;

  private Camera mCamera = null;
  private boolean mFlashlightOn = false;
  private String mSmsName = null;
  private String mSmsPhoneNumber = null;
  private String mSmsMessage = null;

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

    mActions = new HashMap<>();

    mDetailsTextViews = new HashMap<>();
    mDetailsTextViews.put(Constants.PRESS_SINGLE, (TextView) findViewById(R.id.detailsSingle));
    mDetailsTextViews.put(Constants.PRESS_DOUBLE, (TextView) findViewById(R.id.detailsDouble));
    mDetailsTextViews.put(Constants.PRESS_LONG, (TextView) findViewById(R.id.detailsLong));

    mReceiver = new MediaReceiver();
    mIntentFilter = new IntentFilter();
    mIntentFilter.addAction("com.andrew.apollo.metachanged");
    registerReceiver(mReceiver, mIntentFilter);

    ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.options, android.R.layout.simple_spinner_item);
    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    Spinner spinnerSingle = (Spinner) findViewById(R.id.spinnerSingle);
    spinnerSingle.setAdapter(adapter);
    spinnerSingle.setOnItemSelectedListener(this);
    spinnerSingle.setSelection(2);
    mActions.put(Constants.PRESS_SINGLE, Constants.ACTION_NEXT);
    mDetailsTextViews.get(Constants.PRESS_SINGLE).setText("");

    Spinner spinnerDouble = (Spinner) findViewById(R.id.spinnerDouble);
    spinnerDouble.setAdapter(adapter);
    spinnerDouble.setOnItemSelectedListener(this);
    spinnerDouble.setSelection(3);
    mActions.put(Constants.PRESS_DOUBLE, Constants.ACTION_PREV);
    mDetailsTextViews.get(Constants.PRESS_DOUBLE).setText("");

    Spinner spinnerLong = (Spinner) findViewById(R.id.spinnerLong);
    spinnerLong.setAdapter(adapter);
    spinnerLong.setOnItemSelectedListener(this);
    spinnerLong.setSelection(1);
    mActions.put(Constants.PRESS_LONG, Constants.ACTION_PLAYPAUSE);
    mDetailsTextViews.get(Constants.PRESS_LONG).setText("");
  }

  public void testSingle(View v) {
    doAction(Constants.PRESS_SINGLE);
  }

  public void testDouble(View v) {
    doAction(Constants.PRESS_DOUBLE);
  }

  public void testLong(View v) {
    doAction(Constants.PRESS_LONG);
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
    else if (requestCode == REQUEST_CONTACT) {
      if (resultCode == RESULT_OK) {

        Uri uri = data.getData();
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();

        int nameIndex = cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME);
        mSmsName = cursor.getString(nameIndex);

        int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
        mSmsPhoneNumber = cursor.getString(phoneIndex);

        final EditText input = new EditText(this);
        AlertDialog dialog = new AlertDialog.Builder(this)
          .setTitle("Text to Send")
          .setView(input)
          .setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              mSmsMessage = input.getText().toString();
              if (mSmsMessage.equals(""))
                mSmsMessage = null;
              mDetailsTextViews.get(mActionPendingResult).setText("Send \"" + mSmsMessage + "\" to " + mSmsName);
              mActionPendingResult = -1;
            }
          })
          .setCancelable(false)
          .create();
        dialog.show();
      }
    }
  }

  public void selectBluetoothDevice() {

    ArrayAdapter<String> deviceArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line);

    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    final ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
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

//    Log.d("oscar", "set new action");

    // get press type
    int pressType = -1;
    if (parent.getId() == R.id.spinnerSingle)
      pressType = Constants.PRESS_SINGLE;
    else if (parent.getId() == R.id.spinnerDouble)
      pressType = Constants.PRESS_DOUBLE;
    else if (parent.getId() == R.id.spinnerLong)
      pressType = Constants.PRESS_LONG;

    // get action type
    int actionType = -1;
    if (pos == 1) {
      actionType = Constants.ACTION_PLAYPAUSE;
      mDetailsTextViews.get(pressType).setText("");
    }
    else if (pos == 2) {
      actionType = Constants.ACTION_NEXT;
      mDetailsTextViews.get(pressType).setText("");
    }
    else if (pos == 3) {
      actionType = Constants.ACTION_PREV;
      mDetailsTextViews.get(pressType).setText("");
    }
    else if (pos == 4) {
      actionType = Constants.ACTION_TOGGLEFLASHLIGHT;
      mDetailsTextViews.get(pressType).setText("");
    }
    else if (pos == 5) {
      actionType = Constants.ACTION_SENDSMS;
      mActionPendingResult = pressType;
      Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
      contactPickerIntent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
      startActivityForResult(contactPickerIntent, REQUEST_CONTACT);
    }

    // store action for press
    mActions.remove(pressType);
    mActions.put(pressType, actionType);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {}

  @Override
  public void onPause() {
    super.onPause();
//    Log.d("oscar", "onPause(): unregister");
    unregisterReceiver(mReceiver);
  }

  @Override
  public void onResume() {
    super.onResume();
//    Log.d("oscar", "onResume(): re-register");
    registerReceiver(mReceiver, mIntentFilter);
  }

  public void doAction(int pressType) {

    // get action type
    int actionType = mActions.get(pressType);

    // do nothing if that is the action type
    if (actionType == Constants.ACTION_DONOTHING)
      return;

    // do action based on action type
    switch (actionType) {
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
      case Constants.ACTION_SENDSMS:
        sendSms();
        break;
    }
  }

  public void sendSms() {
    if ((mSmsPhoneNumber != null) && (mSmsMessage != null))
      SmsManager.getDefault().sendTextMessage(mSmsPhoneNumber, null, mSmsMessage, null, null);
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

//    Log.d("oscar", "sending media intent");

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
