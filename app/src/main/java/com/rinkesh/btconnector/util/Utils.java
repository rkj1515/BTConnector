package com.rinkesh.btconnector.util;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.Toast;

import com.rinkesh.btconnector.R;

/**
 * Created by rinkesh on 04/03/17.
 */

public class Utils {
    public static final String GENERAL_DB_NAME = "contact_db";
    public static final int BLUETOOTH_ENABLE = 100;
    public static int lastState;
    public static Intent speechIntent;

    public static boolean isCallActive(Context context) {
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return manager.getMode() == AudioManager.MODE_RINGTONE;
    }

    public static boolean enableBluetooth(Activity mainScreen) {
        try {
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if ((mBluetoothAdapter == null)) {
                Toast.makeText(mainScreen.getApplicationContext(), mainScreen.getString(R.string.BLUETOOTH_NOT_SUPPORTED_ON_YOUR_DEVICE), Toast.LENGTH_SHORT).show();
                mainScreen.finish();
                return false;
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                mainScreen.startActivityForResult(enableBtIntent, BLUETOOTH_ENABLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
