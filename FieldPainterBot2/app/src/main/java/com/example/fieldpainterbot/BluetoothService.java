package com.example.fieldpainterbot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.List;

import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

public class BluetoothService {
    private final BluetoothAdapter adapter; // Adapter to manage Bluetooth operations
    private final MutableLiveData<ConnectionStatus> connectionStatus; // LiveData to observe and update the connection status
    private final List<BluetoothDevice> foundDevices = new ArrayList<>(); // List to store discovered Bluetooth devices
    
    // Constructor initializes the Bluetooth adapter and sets up a BroadcastReceiver to listen for discovered Bluetooth devices
    // parameter context             Application context for registering the receiver
    // parameter devices             LiveData to post discovered devices
    // parameter connectionStatus    LiveData to track connection status
    public BluetoothService(Context context, MutableLiveData<List<BluetoothDevice>> devices,
                            MutableLiveData<ConnectionStatus> connectionStatus) {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.connectionStatus = connectionStatus;
        // create an intent filter for Bluetooth device discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // Define a BroadcasterReceiver to handle discovered devices
        BroadcastReceiver receiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !foundDevices.contains(device)) { // add the device to the list if its not already there
                    foundDevices.add(device);
                    devices.postValue(new ArrayList<>(foundDevices)); // post updated list to LiveData for observers
                }
            }
        };
        // Receiver listen for discovered devices
        context.registerReceiver(receiver, filter);
    }
    // Starts Bluetooth device discovery after checking for necessary permissions
    // parameter context    Application context for permission checks
    @SuppressLint("MissingPermission")
    public void startDiscovery(Context context) {
        foundDevices.clear(); // clear previous discovery results
        // For Android 12+, check if BLUETOOTH_SCAN permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
            // If permission is not granted, update connection status and exit
            connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
            return;
        }
        // Start Bluetooth discovery
        adapter.startDiscovery();
    }
    // Simulates connection
    public void connect(BluetoothDevice device) {

        connectionStatus.postValue(ConnectionStatus.CONNECTED);
    }

}
