package com.example.fieldpainterbot;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

public class ConnectionViewModel extends AndroidViewModel {
    private final MutableLiveData<List<BluetoothDevice>> devices = new MutableLiveData<>();
    private final MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>();
    private final BluetoothService bluetoothService;

    public ConnectionViewModel(@NonNull Application application) {
        super(application);
        bluetoothService = new BluetoothService(application, devices, connectionStatus);
    }

    public LiveData<List<BluetoothDevice>> getDevices() {
        return devices;
    }

    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }

    public void startDiscovery(Context context) {
        bluetoothService.startDiscovery(context);
    }

    public void connectToDevice(BluetoothDevice device) {
        bluetoothService.connect(device);
    }
}
