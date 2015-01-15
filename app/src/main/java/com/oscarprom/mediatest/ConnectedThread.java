package com.oscarprom.mediatest;

import android.util.Log;
import java.io.IOException;
import java.io.InputStream;

public class ConnectedThread extends Thread {

  private final InputStream mInputStream;

  private MainActivity mCallingActivity;

  public ConnectedThread(MainActivity activity) {

    mCallingActivity = activity;

    InputStream tempInputStream = null;
    try {
      tempInputStream = mCallingActivity.mBluetoothSocket.getInputStream();
    }
    catch (IOException ioe) {
      ioe.printStackTrace();
    }

    mInputStream = tempInputStream;
  }

  public void run() {

    byte[] buffer = new byte[1024];
    int bytes;

    while (true) {

      try {

        bytes = mInputStream.read(buffer);

        if (bytes > 0) {

          // single press
          if (buffer[0] == 83) {
            Log.d("oscar", "Single press");
            mCallingActivity.doAction(Constants.PRESS_SINGLE);
          }

          // double press
          else if (buffer[0] == 68) {
            Log.d("oscar", "Double press");
            mCallingActivity.doAction(Constants.PRESS_DOUBLE);
          }

          // long press
          else if (buffer[0] == 76) {
            Log.d("oscar", "Long press");
            mCallingActivity.doAction(Constants.PRESS_LONG);
          }
        }
      }
      catch (IOException ioe) {
        ioe.printStackTrace();
        break;
      }
    }
  }
}
