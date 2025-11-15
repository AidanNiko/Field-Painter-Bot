package com.example.fieldpainterbot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class BluetoothService {
    private static final String TAG = "BluetoothService";
    private static final UUID APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static BluetoothService instance; // Singleton instance

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothIOThread ioThread;
    private BluetoothSocket socket;
    private DataListener dataListener;

    private final MutableLiveData<List<BluetoothDevice>> devicesLiveData;
    private final MutableLiveData<ConnectionStatus> connectionStatus;

    // Private constructor (singleton pattern)
    private BluetoothService(Context context,
                             MutableLiveData<List<BluetoothDevice>> devices,
                             MutableLiveData<ConnectionStatus> connectionStatus) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.devicesLiveData = devices;
        this.connectionStatus = connectionStatus;
    }

    // Singleton getter
    public static synchronized BluetoothService getInstance(
            Context context,
            MutableLiveData<List<BluetoothDevice>> devices,
            MutableLiveData<ConnectionStatus> connectionStatus
    ) {
        // Prevent creating instance if Bluetooth is not supported
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.w(TAG, "Bluetooth NOT supported on this device/emulator.");
            return null;
        }

        if (instance == null) {
            instance = new BluetoothService(context.getApplicationContext(), devices, connectionStatus);
        }
        return instance;
    }


    public void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Bluetooth discovery started");
        } else {
            Log.w(TAG, "Bluetooth adapter not available or disabled");
        }
    }

    public void connect(BluetoothDevice device) {
        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(APP_UUID);
                bluetoothAdapter.cancelDiscovery();
                socket.connect();

                connectionStatus.postValue(ConnectionStatus.CONNECTED);
                Log.d(TAG, "Connected to " + device.getName());

                ioThread = new BluetoothIOThread(socket, dataListener);
                ioThread.start();
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
                closeSocket();
            }
        }).start();
    }

    public void send(String message) {
        if (ioThread != null) {
            ioThread.send(message);
        } else {
            Log.w(TAG, "Attempted to send but no active connection");
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

    public void setDataListener(DataListener listener) {
        this.dataListener = listener;
    }

    public interface DataListener {
        void onBatteryLevelReceived(int level);

        void onSprayLevelReceived(int level);

        void onProgressLevelReceived(int level);
    }
}
