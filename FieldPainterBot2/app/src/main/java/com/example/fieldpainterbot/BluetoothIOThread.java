package com.example.fieldpainterbot;

import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Thread that manages I/O over a connected BluetoothSocket.
 * Can send and receive data from the remote device.
 */
public class BluetoothIOThread extends Thread {
    private static final String TAG = "BluetoothIOThread";

    private final BluetoothSocket socket;
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private volatile boolean running = true;

    public BluetoothIOThread(BluetoothSocket socket) throws IOException {
        this.socket = socket;
        this.inputStream = socket.getInputStream();
        this.outputStream = socket.getOutputStream();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[1024]; // Buffer for incoming data

        while (running) {
            try {
                int bytesRead = inputStream.read(buffer);
                if (bytesRead > 0) {
                    String received = new String(buffer, 0, bytesRead);
                    Log.d(TAG, "Received: " + received);

                    // TODO: Handle incoming data (e.g., update UI or state)
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from input stream", e);
                break;
            }
        }

        close();
    }

    /**
     * Sends a string message to the connected device.
     *
     * @param message The message to send
     */
    public void send(String message) {
        try {
            outputStream.write(message.getBytes());
            outputStream.flush();
            Log.d(TAG, "Sent: " + message);
        } catch (IOException e) {
            Log.e(TAG, "Error writing to output stream", e);
        }
    }

    /**
     * Stops the thread and closes the socket.
     */
    public void close() {
        running = false;
        try {
            socket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}