package com.example.fieldpainterbot;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothIOThread extends Thread {
    private static final String TAG = "BluetoothIOThread";

    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final BluetoothService.DataListener dataListener;
    private volatile boolean running = true;

    public BluetoothIOThread(BluetoothSocket socket, BluetoothService.DataListener listener) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
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

                    // Example: If data looks like "BATTERY:85"
                    if (received.startsWith("BATTERY:")) {
                        try {
                            int level = Integer.parseInt(received.split(":")[1]);
                            if (dataListener != null) {
                                dataListener.onBatteryLevelReceived(level);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid battery format: " + received);
                        }
                    }
                    // Example: If data looks like "SPRAY:85"
                    if (received.startsWith("SPRAY:")) {
                        try {
                            int level = Integer.parseInt(received.split(":")[1]);
                            if (dataListener != null) {
                                dataListener.onSprayLevelReceived(level);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid spray format: " + received);
                        }
                    }
                    if (received.startsWith("PROGRESS:")) {
                        try {
                            int level = Integer.parseInt(received.split(":")[1]);
                            if (dataListener != null) {
                                dataListener.onProgressLevelReceived(level);
                            }
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Invalid progress format: " + received);
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading input stream", e);
                break;
            }
        }

        close();
    }

    public void send(String message) {
        try {
            outputStream.write(message.getBytes());
            outputStream.flush();
            Log.d(TAG, "Sent: " + message);
        } catch (IOException e) {
            Log.e(TAG, "Error sending message", e);
        }
    }

    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}
