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
    private final BluetoothAdapter adapter;
    private final MutableLiveData<List<BluetoothDevice>> devices;
    private final MutableLiveData<ConnectionStatus> connectionStatus;
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();

    public BluetoothService(Context context, MutableLiveData<List<BluetoothDevice>> devices,
                            MutableLiveData<ConnectionStatus> connectionStatus) {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.devices = devices;
        this.connectionStatus = connectionStatus;

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, filter);
    }

    @SuppressLint("MissingPermission")
    public void startDiscovery(Context context) {
        foundDevices.clear();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {

            connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
            return;
        }

        adapter.startDiscovery();
    }

    public void connect(BluetoothDevice device) {

        connectionStatus.postValue(ConnectionStatus.CONNECTED);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if (device != null && !foundDevices.contains(device)) {
                foundDevices.add(device);
                devices.postValue(new ArrayList<>(foundDevices));
            }
        }
    };
}
