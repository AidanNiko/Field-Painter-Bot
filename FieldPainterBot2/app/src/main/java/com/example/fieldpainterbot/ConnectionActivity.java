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
        
        Log.d("ConnectionActivity", "onCreate() started");

        try {
            viewModel = ConnectionViewModel.getInstance(getApplication());
            Log.d("ConnectionActivity", "ViewModel obtained");
            
            viewModel.stopDiscovery();
            Log.d("ConnectionActivity", "stopDiscovery() called");
            
            // Reset connection status to DISCONNECTED for fresh start
            viewModel.resetConnectionStatus();
            Log.d("ConnectionActivity", "Connection status reset to DISCONNECTED");
            
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Log.d("ConnectionActivity", "BluetoothAdapter obtained");
        } catch (Exception e) {
            Log.e("ConnectionActivity", "Error during initialization", e);
            return;
        }

        // No Bluetooth hardware
        // Handle Skip button
        Log.d("ConnectionActivity", "Looking for skip button");
        Button skipButton = findViewById(R.id.skipped);
        Log.d("ConnectionActivity", "Skip button found: " + (skipButton != null));
        if (skipButton != null) {
            skipButton.setOnClickListener(v -> {
                Intent intent = new Intent(ConnectionActivity.this, DashboardActivity.class);
                startActivity(intent);
            });
        }

        // Handle Refresh button
        Log.d("ConnectionActivity", "Looking for refresh button");
        FloatingActionButton refreshButton = findViewById(R.id.btnRefresh);
        Log.d("ConnectionActivity", "Refresh button found: " + (refreshButton != null));
        if (refreshButton != null) {
            refreshButton.setOnClickListener(v -> {
                Toast.makeText(this, "Refreshing devices...", Toast.LENGTH_SHORT).show();

                // Stop and restart discovery (clean refresh)
                viewModel.stopDiscovery();
                viewModel.startDiscovery();
            });
        }


        // Check Bluetooth availability
        Log.d("ConnectionActivity", "Checking Bluetooth availability");
        if (bluetoothAdapter == null) {
            Log.w("ConnectionActivity", "Bluetooth adapter is null (emulator or no BT hardware)");
            // Don't return - allow the activity to continue without BT for testing
            // Connection will stay DISCONNECTED by default
        } else {
            Log.d("ConnectionActivity", "Bluetooth available");
            
            // If Bluetooth is OFF → ask the user to turn it on
            Log.d("ConnectionActivity", "Checking if Bluetooth is enabled");
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBluetoothLauncher.launch(enableIntent);
            } else {
                Log.d("ConnectionActivity", "Bluetooth already enabled, requesting permissions");
                checkAndRequestPermissions();
            }
        }
        Log.d("ConnectionActivity", "Bluetooth check completed");

        // RecyclerView setup
        Log.d("ConnectionActivity", "Setting up RecyclerView");
        RecyclerView recyclerView = findViewById(R.id.deviceList);
        Log.d("ConnectionActivity", "RecyclerView found: " + (recyclerView != null));
        
        DeviceAdapter adapter = new DeviceAdapter(device -> {
            // Correct: explicitly connect only when the user taps a device
            viewModel.connectToDevice(device);
        });
        Log.d("ConnectionActivity", "Adapter created");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Log.d("ConnectionActivity", "Layout manager set");
        
        recyclerView.setAdapter(adapter);
        Log.d("ConnectionActivity", "Adapter set");
        
        recyclerView.addItemDecoration(new DividerItemDecoration(this, LinearLayoutManager.VERTICAL));
        Log.d("ConnectionActivity", "Item decoration added");

        // Observe list of discovered devices
        viewModel.getDevices().observe(this, adapter::submitList);
        Log.d("ConnectionActivity", "Device observer set");

        // Observe connection state (ONLY changes when BluetoothService reports it)
        Log.d("ConnectionActivity", "Setting up connection status observer");
        viewModel.getConnectionStatus().observe(this, status -> {
            Log.d("ConnectionActivity", "Connection status observed: " + (status != null ? status.toString() : "NULL"));
            Log.d("ConnectionActivity", "Is CONNECTED? " + (status == ConnectionStatus.CONNECTED));
            if (status == ConnectionStatus.CONNECTED) {
                Toast.makeText(this, "Connected!", Toast.LENGTH_SHORT).show();
                Log.d("ConnectionActivity", "Starting DashboardActivity");
                startActivity(new Intent(this, DashboardActivity.class));
            } else {
                Log.d("ConnectionActivity", "Not connected yet. Status: " + status);
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
