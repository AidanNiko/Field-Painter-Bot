package com.example.fieldpainterbot;

import android.bluetooth.BluetoothDevice;
// Functional interface used to handle clicks on Bluetooth devices in a list.
// Implemented by classes that want to respond when a user selects a Bluetooth device from the UI
public interface OnDeviceClickListener {
    // Callback method triggered when a Bluetooth device is clicked
    void onDeviceClick(BluetoothDevice device);
}
