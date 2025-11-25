package com.example.fieldpainterbot;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class BluetoothService {

    private static final String TAG = "BluetoothService";
    private static final UUID APP_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String SERVICE_NAME = "FieldPainter";

    private static BluetoothService instance;

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothIOThread ioThread;
    private BluetoothSendThread sendThread;
    private BluetoothSocket socket;
    private BluetoothServerThread serverThread;
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
        if (instance == null) {
            instance = new BluetoothService(
                    context.getApplicationContext(),
                    devices,
                    connectionStatus
            );
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
                          CLIENT CONNECT
       ------------------------------------------------------- */

    public void connect(BluetoothDevice device) {
        if (socket != null && socket.isConnected()) return;

        connectionStatus.postValue(ConnectionStatus.CONNECTING);

        new Thread(() -> {
            try {
                bluetoothAdapter.cancelDiscovery();
                socket = device.createInsecureRfcommSocketToServiceRecord(APP_UUID);
                socket.connect();  // <-- blocks until Pi accepts
                Log.e(TAG, "Connected");

                connectionStatus.postValue(ConnectionStatus.CONNECTED);
                Log.e(TAG, "Connected is included to status");
                startIOThreads(socket);

            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
                closeSocket();
            }
        }).start();
    }

    /* -------------------------------------------------------
                          SERVER MODE
       ------------------------------------------------------- */

    public void startServer() {
        if (serverThread == null) {
            serverThread = new BluetoothServerThread();
            serverThread.start();
        }
    }

    private class BluetoothServerThread extends Thread {
        private final BluetoothServerSocket serverSocket;

        public BluetoothServerThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, APP_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Server socket creation failed", e);
            }
            serverSocket = tmp;
            connectionStatus.postValue(ConnectionStatus.LISTENING);
        }

        @Override
        public void run() {
            BluetoothSocket clientSocket;
            while (true) {
                try {
                    clientSocket = serverSocket.accept(); // blocks until a device connects
                    if (clientSocket != null) {
                        Log.d(TAG, "Device connected: " + clientSocket.getRemoteDevice().getName());
                        socket = clientSocket;
                        connectionStatus.postValue(ConnectionStatus.CONNECTED);
                        startIOThreads(clientSocket);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Accept failed", e);
                    break;
                }
            }
        }

        public void cancel() {
            try { serverSocket.close(); } catch (IOException ignored) {}
        }
    }

    /* -------------------------------------------------------
                          I/O THREADS
       ------------------------------------------------------- */

    private void startIOThreads(BluetoothSocket socket) throws IOException {
        ioThread = new BluetoothIOThread(socket, dataListener);
        ioThread.start();

        sendThread = new BluetoothSendThread(socket);
        sendThread.start();
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

        if (serverThread != null) {
            serverThread.cancel();
            serverThread = null;
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
