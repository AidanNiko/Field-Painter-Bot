package com.example.fieldpainterbot;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service class that manages Bluetooth discovery and connection.
 * Uses LiveData to notify observers of discovered devices and connection status.
 */
public class BluetoothService {
    private static final String TAG = "BluetoothService";

    // Standard UUID for Serial Port Profile (SPP)
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final BluetoothAdapter adapter;
    private final MutableLiveData<ConnectionStatus> connectionStatus;
    private final List<BluetoothDevice> foundDevices = new ArrayList<>();
    private BluetoothSocket socket;

    private BluetoothIOThread ioThread;

    private final BroadcastReceiver receiver;

    /**
     * Constructor sets up Bluetooth adapter and discovery receiver.
     *
     * @param context           Application context
     * @param devicesLiveData   LiveData to post discovered devices
     * @param connectionStatus  LiveData to post connection status
     */
    public BluetoothService(Context context,
                            MutableLiveData<List<BluetoothDevice>> devicesLiveData,
                            MutableLiveData<ConnectionStatus> connectionStatus) {
        this.adapter = BluetoothAdapter.getDefaultAdapter();
        this.connectionStatus = connectionStatus;

        // BroadcastReceiver to handle discovered devices
        this.receiver = new BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device != null && !foundDevices.contains(device)) {
                        foundDevices.add(device);
                        devicesLiveData.postValue(new ArrayList<>(foundDevices));
                    }
                }
            }
        };

        // Register receiver for device discovery
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(receiver, filter);
    }

    /**
     * Starts Bluetooth device discovery after checking permissions.
     *
     * @param context Application context
     */
    @SuppressLint("MissingPermission")
    public void startDiscovery(Context context) {
        foundDevices.clear();

        if (!hasScanPermission(context)) {
            connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
            Log.w(TAG, "Missing BLUETOOTH_SCAN permission");
            return;
        }

        try {
            if (adapter.isDiscovering()) {
                adapter.cancelDiscovery();
            }
            adapter.startDiscovery();
            Log.d(TAG, "Discovery started");
        } catch (SecurityException e) {
            Log.e(TAG, "Bluetooth discovery failed due to missing permission", e);
            connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
        }
    }


    @SuppressLint("MissingPermission")
    public void connect(Context context, BluetoothDevice device) {
        connectionStatus.postValue(ConnectionStatus.CONNECTING);

        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                                != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Missing BLUETOOTH_CONNECT permission");
                    connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
                    return;
                }

                socket = device.createRfcommSocketToServiceRecord(SPP_UUID);
                adapter.cancelDiscovery();
                socket.connect();

                connectionStatus.postValue(ConnectionStatus.CONNECTED);
                Log.i(TAG, "Connected to " + device.getName());

                ioThread = new BluetoothIOThread(socket);
                ioThread.start();

            } catch (SecurityException e) {
                Log.e(TAG, "Bluetooth connection failed due to missing permission", e);
                connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
                closeSocket();
            }
        }).start();
    }

    public void sendMessage(String message) {
        if (ioThread != null) {
            ioThread.send(message);
        } else {
            Log.w(TAG, "Cannot send message — not connected");
        }
    }

    public void disconnect() {
        if (ioThread != null) {
            ioThread.close();
            ioThread = null;
        }
        closeSocket();
        connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
        Log.d(TAG, "Disconnected");
    }

    /**
     * Cleans up the socket connection.
     */
    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }

    /**
     * Checks if the app has permission to scan for Bluetooth devices.
     *
     * @param context Application context
     * @return true if permission is granted
     */
    private boolean hasScanPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
}