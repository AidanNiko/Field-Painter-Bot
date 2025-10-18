package com.example.fieldpainterbot;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;
// manages Bluetooth device discovery and connection status.
public class ConnectionViewModel extends AndroidViewModel {
    private final MutableLiveData<List<BluetoothDevice>> devices = new MutableLiveData<>();
    private final MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>();
    private final BluetoothService bluetoothService;
    
    // Constructor initializes the BluetoothService and binds LiveData references
    public ConnectionViewModel(@NonNull Application application) {
        super(application);
        bluetoothService = new BluetoothService(application, devices, connectionStatus);
    }
    // Exposes the list of discovered devices as immutable LiveData
    // return LiveData of BluetoothDevice list
    public LiveData<List<BluetoothDevice>> getDevices() {
        return devices;
    }
    // Exposes the current connection status as immutable LiveData
    // return LiveData of ConnectionStatus
    public LiveData<ConnectionStatus> getConnectionStatus() {
        return connectionStatus;
    }
    // Starts Bluetooth device discovery via service 
    // parameter context required for permission checks and broadcast registration
    public void startDiscovery(Context context) {
        bluetoothService.startDiscovery(context);
    }
    // Initiates connection to a selected Bluetooth device
    // parameter device the BluetoothDevice to connect to
    public void connectToDevice(BluetoothDevice device) {
        bluetoothService.connect(device);
    }
}
