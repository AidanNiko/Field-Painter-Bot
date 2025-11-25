package com.example.fieldpainterbot;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ConnectionViewModel extends AndroidViewModel {

    private static ConnectionViewModel instance;

    private final MutableLiveData<List<BluetoothDevice>> devices = new MutableLiveData<>();
    private final MutableLiveData<ConnectionStatus> connectionStatus = new MutableLiveData<>();
    private final MutableLiveData<Integer> batteryLevel = new MutableLiveData<>();
    private final MutableLiveData<Integer> sprayLevel = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressLevel = new MutableLiveData<>();

    private final BluetoothService bluetoothService;
    private final BluetoothAdapter bluetoothAdapter;
    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();

    private ConnectionViewModel(@NonNull Application application) {
        super(application);

        bluetoothService = BluetoothService.getInstance(application, devices, connectionStatus);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothService != null) {
            bluetoothService.setDataListener(new BluetoothService.DataListener() {
                @Override
                public void onBatteryLevelReceived(int level) { batteryLevel.postValue(level); }
                @Override
                public void onSprayLevelReceived(int level) { sprayLevel.postValue(level); }
                @Override
                public void onProgressLevelReceived(int level) { progressLevel.postValue(level); }
            });
        }

        registerBluetoothStateReceiver(application);
        registerDeviceDiscoveryReceiver(application);
    }

    public static synchronized ConnectionViewModel getInstance(@NonNull Application application) {
        if (instance == null) {
            instance = new ConnectionViewModel(application);
        }
        return instance;
    }

    /* ------------------ STATE RECEIVER ------------------ */
    private void registerBluetoothStateReceiver(Context context) {
        BroadcastReceiver receiver = new BroadcastReceiver() {
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
                        connectionStatus.postValue(ConnectionStatus.READY);
                        startDiscovery(); // auto scan
                        break;
                }
            }
        };
        context.registerReceiver(receiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    /* ------------------ DEVICE DISCOVERY ------------------ */
    private void registerDeviceDiscoveryReceiver(Context context) {
        BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Only add devices with a name and not "Unnamed Device"
                    if (device != null
                            && device.getName() != null
                            && !device.getName().equals("Unnamed Device")
                            && !discoveredDevices.contains(device)) {
                        discoveredDevices.add(device);
                        devices.postValue(new ArrayList<>(discoveredDevices));
                    }
                }
            }
        };
        context.registerReceiver(discoveryReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
    }


    public LiveData<List<BluetoothDevice>> getDevices() { return devices; }
    public LiveData<ConnectionStatus> getConnectionStatus() { return connectionStatus; }
    public LiveData<Integer> getBatteryLevel() { return batteryLevel; }
    public LiveData<Integer> getSprayLevel() { return sprayLevel; }
    public LiveData<Integer> getProgressLevel() { return progressLevel; }

    public void startDiscovery() {
        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
            discoveredDevices.clear();
            devices.postValue(new ArrayList<>());
            bluetoothAdapter.startDiscovery();
            Log.d("DISCOVERY", "Bluetooth discovery started");
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (bluetoothService != null) bluetoothService.connect(device);
    }

    public void disconnect() {
        if (bluetoothService != null) bluetoothService.disconnect();
    }

    /* ------------------ UTILITY ------------------ */
    private void runOnMain(Runnable runnable) {
        if (runnable == null) return;
        android.os.Handler mainHandler = new android.os.Handler(getApplication().getMainLooper());
        mainHandler.post(runnable);
    }

    public void fetchFieldAndSend(String fieldName, Runnable onComplete, Runnable onError) {

        FieldDatabaseConnection db = new FieldDatabaseConnection();

        db.fetchData(fieldName, data -> {

            if (bluetoothService == null) {
                runOnMain(onError);
                return;
            }

            bluetoothService.send(
                    data,
                    () -> runOnMain(onComplete),
                    () -> runOnMain(onError)
            );

        });
    }

    public void sendControlCommand(String command, String state) {
        try {
            JSONObject obj = new JSONObject();
            obj.put("command", command);
            obj.put("state", state);

            bluetoothService.send(
                    obj.toString(),
                    () -> Log.d("SEND", "Sent: " + obj),
                    () -> Log.e("SEND", "Failed to send: " + obj)
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
