package com.example.fieldpainterbot;

import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class BluetoothSendThread extends Thread {
    private final OutputStream outputStream;
    private final BlockingQueue<String> sendQueue = new LinkedBlockingQueue<>();
    private volatile boolean running = true;

    public BluetoothSendThread(BluetoothSocket socket) throws IOException {
        this.outputStream = socket.getOutputStream();
    }

    public void enqueueMessage(String msg) {
        sendQueue.add(msg);
    }

    @Override
    public void run() {
        while (running) {
            try {
                String msg = sendQueue.take();  // waits until message exists
                outputStream.write(msg.getBytes());
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void close() {
        running = false;
        this.interrupt();
    }
}

