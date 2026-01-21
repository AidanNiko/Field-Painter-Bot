package com.example.fieldpainterbot;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Button;

public class ConnectionActivity extends AppCompatActivity {

    private ConnectionViewModel viewModel;
    private BluetoothAdapter bluetoothAdapter;

    // Launcher for enabling Bluetooth popup
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                    checkAndRequestPermissions();
                } else {
                    Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        viewModel = ConnectionViewModel.getInstance(getApplication());
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // No Bluetooth hardware
        // Handle Skip button
        Button skipButton = findViewById(R.id.skipped);
        skipButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConnectionActivity.this, DashboardActivity.class);
            startActivity(intent);
        });

        // Handle Refresh button
        FloatingActionButton refreshButton = findViewById(R.id.btnRefresh);
        refreshButton.setOnClickListener(v -> {
            Toast.makeText(this, "Refreshing devices...", Toast.LENGTH_SHORT).show();

            // Stop and restart discovery (clean refresh)
            viewModel.stopDiscovery();
            viewModel.startDiscovery();
        });


        // Check Bluetooth availability
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Skip button
        findViewById(R.id.skipped).setOnClickListener(v -> {
            startActivity(new Intent(this, DashboardActivity.class));
        });

        // If Bluetooth is OFF → ask the user to turn it on
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableIntent);
        } else {
            checkAndRequestPermissions();
        }

        // RecyclerView setup
        RecyclerView recyclerView = findViewById(R.id.deviceList);
        DeviceAdapter adapter = new DeviceAdapter(device -> {
            // Correct: explicitly connect only when the user taps a device
            viewModel.connectToDevice(device);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));

        // Observe list of discovered devices
        viewModel.getDevices().observe(this, adapter::submitList);

        // Observe connection state (ONLY changes when BluetoothService reports it)
        viewModel.getConnectionStatus().observe(this, status -> {
            Log.d("UI", "Connection status changed: " + status);
            if (status == ConnectionStatus.CONNECTED) {
                Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, DashboardActivity.class));
            }
        });
    }

    /**
     * Request required permissions AFTER Bluetooth is turned on
     */
    private void checkAndRequestPermissions() {

        // Android 12+ requires SCAN + CONNECT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            boolean missingPermissions =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                            != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                                    != PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED;

            if (missingPermissions) {
                ActivityCompat.requestPermissions(this,
                        new String[] {
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, 1);
                Log.d("PERM", "Requesting permissions...");

                return;
            }

        } else {
            // Android 6–11 only requires Location
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, 1);
                return;
            }
        }

        // If we reach here → permissions OK
        viewModel.startDiscovery();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            if (granted) {
                viewModel.startDiscovery();
            } else {
                Toast.makeText(this, "Permissions denied — cannot scan for devices.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
