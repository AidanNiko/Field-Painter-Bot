package com.example.fieldpainterbot;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class ConnectionViewModel extends AndroidViewModel {

    private final MutableLiveData<List<BluetoothDevice>> devices = new MutableLiveData<>();
    private final MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>();

    private final MutableLiveData<Integer> batteryLevel = new MutableLiveData<>();
    private final MutableLiveData<Integer> sprayLevel = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressLevel = new MutableLiveData<>();

    private final BluetoothService bluetoothService;

    /* -------------------------------------------------------
                        CONSTRUCTOR
       ------------------------------------------------------- */
    public ConnectionViewModel(@NonNull Application application) {
        super(application);

        bluetoothService = BluetoothService.getInstance(application, devices, connectionStatus);

        if (bluetoothService != null) {
            bluetoothService.setDataListener(new BluetoothService.DataListener() {
                @Override
                public void onBatteryLevelReceived(int level) {
                    batteryLevel.postValue(level);
                }

                @Override
                public void onSprayLevelReceived(int level) {
                    sprayLevel.postValue(level);
                }

                @Override
                public void onProgressLevelReceived(int level) {
                    progressLevel.postValue(level);
                }
            });
        }

        // Set initial connection status
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            connectionStatus.setValue(ConnectionStatus.DISCONNECTED);
        } else {
            connectionStatus.setValue(ConnectionStatus.CONNECTED);
        }

        // Listen to Bluetooth ON/OFF
        application.registerReceiver(
                new BluetoothStateReceiver(),
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        );
    }

    /* -------------------------------------------------------
                    BLUETOOTH STATUS RECEIVER
       ------------------------------------------------------- */
    private class BluetoothStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) return;

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                    connectionStatus.postValue(ConnectionStatus.DISCONNECTED);
                    break;

                case BluetoothAdapter.STATE_ON:
                case BluetoothAdapter.STATE_TURNING_ON:
                    connectionStatus.postValue(ConnectionStatus.CONNECTED);
                    break;
            }
        }
    }

    /* -------------------------------------------------------
                        LIVE DATA GETTERS
       ------------------------------------------------------- */
    public LiveData<List<BluetoothDevice>> getDevices() { return devices; }
    public LiveData<ConnectionStatus> getConnectionStatus() { return connectionStatus; }

    public LiveData<Integer> getBatteryLevel() { return batteryLevel; }
    public LiveData<Integer> getSprayLevel() { return sprayLevel; }
    public LiveData<Integer> getProgressLevel() { return progressLevel; }

    /* -------------------------------------------------------
                        BLUETOOTH ACTIONS
       ------------------------------------------------------- */
    public void startDiscovery() {
        if (bluetoothService != null) bluetoothService.startDiscovery();
    }

    public void connectToDevice(BluetoothDevice device) {
        if (bluetoothService != null) bluetoothService.connect(device);
    }

    public void disconnect() {
        if (bluetoothService != null) bluetoothService.disconnect();
    }

    /* -------------------------------------------------------
                FETCH DATA FROM DATABASE + SEND
       ------------------------------------------------------- */
    public void fetchFieldAndSend(
            String fieldName,
            Runnable onComplete,
            Runnable onError
    ) {
        FieldDatabaseConnection db = new FieldDatabaseConnection();

        db.fetchData(fieldName, data -> {

            if (bluetoothService == null) {
                onError.run();
                return;
            }

            bluetoothService.send(
                    data,
                    () -> runOnMain(onComplete),
                    () -> runOnMain(onError)
            );

        });
    }

    /* -------------------------------------------------------
                      MAIN THREAD CALLBACK UTILITY
       ------------------------------------------------------- */
    private void runOnMain(Runnable runnable) {
        if (runnable == null) return;
        android.os.Handler mainHandler = new android.os.Handler(getApplication().getMainLooper());
        mainHandler.post(runnable);
    }
}
