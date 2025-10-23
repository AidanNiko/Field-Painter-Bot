package com.example.fieldpainterbot;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.fieldpainterbot.DeviceAdapter;

import java.util.Objects;
public class ConnectionActivity extends AppCompatActivity {
    private ConnectionViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        //Handle skip button to proceed without connecting a device
        Button skipButton = findViewById(R.id.skipButton);
        skipButton.setOnClickListener(v -> {
            Intent intent = new Intent(ConnectionActivity.this, FieldChoiceActivity.class);
            startActivity(intent); // Navigate to FieldChoiceActivity
        });

        // Initialize the viewmodel for managing bluetooth logic
        viewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);

        // Check and request necessary permissions based on android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For android 12 and above, check for bluetooth scan/connect and location permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Request all required permissions
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.ACCESS_FINE_LOCATION
                        }, 1);
            } else {
                //permissions already granted, start device discovery
                viewModel.startDiscovery(this);
            }
        } else {
            // For Android 12 and below, only location permission is needed
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                viewModel.startDiscovery(this);
            }
        }

        // Setup RecyclerView to display discovered Bluetooth devices
        RecyclerView recyclerView = findViewById(R.id.deviceList);
        DeviceAdapter adapter = new DeviceAdapter(device -> viewModel.connectToDevice(device));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Add visual divider between items in the list
        DividerItemDecoration divider = new DividerItemDecoration(
                recyclerView.getContext(), LinearLayoutManager.VERTICAL);
        divider.setDrawable(Objects.requireNonNull(ContextCompat.getDrawable(this, R.drawable.divider)));
        recyclerView.addItemDecoration(divider);


        // Observe changes in the list of discovered devices and update UI
        viewModel.getDevices().observe(this, adapter::submitList);
        // Observe connection status and navigate to the next screen if connected
        viewModel.getConnectionStatus().observe(this, status -> {
            if (status == ConnectionStatus.CONNECTED) {
                startActivity(new Intent(this, FieldChoiceActivity.class));
            }
        });

    }
    // Callback for handling permission request results.
    // If all permissions are granted, start Bluetooth discovery.
    // Otherwise, show a warning toast.
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (allGranted) {
            viewModel.startDiscovery(this); // permission granted, start discovery
        } else {
            Toast.makeText(this, "Bluetooth scan permission is required.", Toast.LENGTH_LONG).show();
        }
    }
}

