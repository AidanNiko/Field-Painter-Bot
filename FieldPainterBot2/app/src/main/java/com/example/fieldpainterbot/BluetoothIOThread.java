package com.example.fieldpainterbot;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

public class BluetoothIOThread extends Thread {
    private static final String TAG = "BluetoothIOThread";

    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final BluetoothService.DataListener dataListener;
    private volatile boolean running = true;

    public BluetoothIOThread(BluetoothSocket socket, BluetoothService.DataListener listener) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.dataListener = listener;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024];

        while (running) {
            try {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    String received = new String(buffer, 0, bytesRead).trim();
                    Log.d(TAG, "Received: " + received);
                    parseIncoming(received);
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading input stream", e);
                break;
            }
        }

        close();
    }

    private void parseIncoming(String received) {
        try {
            if (received.startsWith("BATTERY:")) {
                dataListener.onBatteryLevelReceived(Integer.parseInt(received.split(":")[1]));
            } else if (received.startsWith("SPRAY:")) {
                dataListener.onSprayLevelReceived(Integer.parseInt(received.split(":")[1]));
            } else if (received.startsWith("PROGRESS:")) {
                dataListener.onProgressLevelReceived(Integer.parseInt(received.split(":")[1]));
            }
        } catch (Exception e) {
            Log.w(TAG, "Invalid format: " + received);
        }
    }

    public void close() {
        running = false;
        try { socket.close(); } catch (IOException ignored) {}
    }
}
