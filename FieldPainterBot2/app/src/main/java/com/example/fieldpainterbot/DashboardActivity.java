package com.example.fieldpainterbot;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.ViewModelStoreOwner;

import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {
    private ImageView batteryIcon;
    private TextView percentLevel;

    private ImageView sprayIcon;

    private TextView percentSpray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Initialize UI references
        batteryIcon = findViewById(R.id.battery_icon);
        percentLevel = findViewById(R.id.percentLevel);
        ImageView bluetoothIcon = findViewById(R.id.bluetooth_icon);

        // Initialize ViewModel (shared Bluetooth logic)
        ConnectionViewModel viewModel = ConnectionViewModel.getInstance(getApplication());



        viewModel.getConnectionStatus().observe(this, status -> {
            Log.d("Testing Dash", "Connection status changed Dashboard: " + status);
            if (status == ConnectionStatus.DISCONNECTED) {
                updateBatteryUI(70);
                updateSprayUI(100);
            }
        });


        //  Observe battery level updates in real-time
        viewModel.getBatteryLevel().observe(this, level -> {
            if (level != null) {
                updateBatteryUI(level);
                Log.d("Battery", "Battery updated: " + level);
            }
            else {
                Log.d("Bluetooth", "Disconnected");
                // Optional: fallback battery level if disconnected
                updateBatteryUI(70);
            }
        });


        sprayIcon = findViewById(R.id.spray_icon);
        percentSpray = findViewById(R.id.sprayLevel);
        viewModel.getSprayLevel().observe(this, level -> {
            if (level != null) {
                updateSprayUI(level);
                Log.d("Spray", "Spray updated: " + level);
            }
            else {
                Log.d("Bluetooth", "Disconnected");
                // Optional: fallback battery level if disconnected
                updateBatteryUI(100);
            }
        });

        // Set up Bluetooth icon click → back to connection screen
        bluetoothIcon.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ConnectionActivity.class);
            startActivity(intent);
        });

        // Set up clickable cards
        MaterialCardView card1 = findViewById(R.id.header_clickable_1);
        MaterialCardView card2 = findViewById(R.id.header_clickable_2);

        card1.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, FieldChoiceActivity.class));
        });

        card2.setOnClickListener(v -> {
            startActivity(new Intent(DashboardActivity.this, ManualControlActivity.class));
        });
    }

    /**
     * Helper function to update battery icon + text based on current level
     */
    private void updateBatteryUI(int batteryLevel) {
        percentLevel.setText(getString(R.string.battery_percent, batteryLevel));

        if (batteryLevel >= 80) {
            batteryIcon.setImageResource(R.drawable.battery_100);
        } else if (batteryLevel >= 60) {
            batteryIcon.setImageResource(R.drawable.battery_75);
        } else if (batteryLevel >= 40) {
            batteryIcon.setImageResource(R.drawable.battery_50);
        } else if (batteryLevel >= 20) {
            batteryIcon.setImageResource(R.drawable.battery_25);
        } else {
            batteryIcon.setImageResource(R.drawable.battery_0);
        }
    }
    private void updateSprayUI(int sprayLevel) {
        percentSpray.setText(getString(R.string.battery_percent, sprayLevel));

        if (sprayLevel >= 80) {
            sprayIcon.setImageResource(R.drawable.spray_100);
        } else if (sprayLevel >= 60) {
            sprayIcon.setImageResource(R.drawable.spray_75);
        } else if (sprayLevel >= 40) {
            sprayIcon.setImageResource(R.drawable.spray_50);
        } else if (sprayLevel >= 20) {
            sprayIcon.setImageResource(R.drawable.spray_25);
        } else {
            sprayIcon.setImageResource(R.drawable.spray_0);
        }
    }
}
