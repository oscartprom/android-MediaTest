package com.oscarprom.mediatest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import java.io.IOException;
import java.util.UUID;

public class ConnectThread extends Thread {

  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

  private final BluetoothSocket mBluetoothSocket;
  private final BluetoothDevice mBluetoothDevice;

  private MainActivity mCallingActivity;

  public ConnectThread(MainActivity activity, BluetoothDevice device) {

    mCallingActivity = activity;
    BluetoothSocket tempSocket = null;
    mBluetoothDevice = device;

    try {
      tempSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }

    mBluetoothSocket = tempSocket;
  }

  public void run() {

    mCallingActivity.mBluetoothAdapter.cancelDiscovery();

    try {
      mBluetoothSocket.connect();
    }
    catch (IOException connectIoe) {
      connectIoe.printStackTrace();
      try {
        mBluetoothSocket.close();
      }
      catch (IOException closeIoe) {
        closeIoe.printStackTrace();
      }
      return;
    }

    mCallingActivity.socketConnected(mBluetoothSocket);
  }

  public void cancel() {
    try {
      mBluetoothSocket.close();
    }
    catch(IOException ioe) {
      ioe.printStackTrace();
    }
  }
}
