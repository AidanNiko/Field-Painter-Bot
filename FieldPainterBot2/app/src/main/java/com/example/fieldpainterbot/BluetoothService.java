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

    private static BluetoothService instance;

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothIOThread ioThread;
    private BluetoothSendThread sendThread;
    private BluetoothSocket socket;
    private DataListener dataListener;

    private final MutableLiveData<List<BluetoothDevice>> devicesLiveData;
    private final MutableLiveData<ConnectionStatus> connectionStatus;

    private BluetoothService(Context context,
                             MutableLiveData<List<BluetoothDevice>> devices,
                             MutableLiveData<ConnectionStatus> connectionStatus) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.devicesLiveData = devices;
        this.connectionStatus = connectionStatus;
    }

    public static synchronized BluetoothService getInstance(
            Context context,
            MutableLiveData<List<BluetoothDevice>> devices,
            MutableLiveData<ConnectionStatus> connectionStatus
    ) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Log.w(TAG, "Bluetooth NOT supported.");
            return null;
        }

        if (instance == null) {
            instance = new BluetoothService(context.getApplicationContext(), devices, connectionStatus);
        }

        return instance;
    }

    /* -------------------------------------------------------
                      DEVICE DISCOVERY
       ------------------------------------------------------- */

    public void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.startDiscovery();
            Log.d(TAG, "Bluetooth discovery started");
        } else {
            Log.w(TAG, "Bluetooth adapter unavailable or disabled");
        }
    }

    /* -------------------------------------------------------
                          CONNECT
       ------------------------------------------------------- */

    public void connect(BluetoothDevice device) {
        new Thread(() -> {
            try {
                socket = device.createRfcommSocketToServiceRecord(APP_UUID);
                bluetoothAdapter.cancelDiscovery();
                socket.connect();

                connectionStatus.postValue(ConnectionStatus.CONNECTED);
                Log.d(TAG, "Connected to " + device.getName());

                // Start IO thread (receiving)
                ioThread = new BluetoothIOThread(socket, dataListener);
                ioThread.start();

                // Start Send thread (queued sending)
                sendThread = new BluetoothSendThread(socket);
                sendThread.start();

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
                closeSocket();
            }
        }).start();
    }

    /* -------------------------------------------------------
                           SEND
       ------------------------------------------------------- */

    public void send(String message, Runnable onSuccess, Runnable onError) {
        if (sendThread != null) {
            try {
                sendThread.enqueueMessage(message);
                onSuccess.run();
            } catch (Exception e) {
                Log.e(TAG, "Send failed", e);
                onError.run();
            }
        } else {
            Log.w(TAG, "Send failed: no connection (sendThread is null)");
            onError.run();
        }
    }



    /* -------------------------------------------------------
                        DISCONNECT
       ------------------------------------------------------- */

    public void disconnect() {

        if (ioThread != null) {
            ioThread.close();
            ioThread = null;
        }

        if (sendThread != null) {
            sendThread.close();
            sendThread = null;
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

    /* -------------------------------------------------------
                     DATA LISTENER SETUP
       ------------------------------------------------------- */

    public void setDataListener(DataListener listener) {
        this.dataListener = listener;
    }

    public interface DataListener {
        void onBatteryLevelReceived(int level);
        void onSprayLevelReceived(int level);
        void onProgressLevelReceived(int level);
    }
}
