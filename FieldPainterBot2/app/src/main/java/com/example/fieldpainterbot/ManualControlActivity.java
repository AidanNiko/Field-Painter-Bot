package com.example.fieldpainterbot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;


public class ManualControlActivity extends AppCompatActivity {
    private ImageView batteryIcon;
    private TextView percentLevel;

    private ImageView sprayIcon;

    private TextView percentSpray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manualcontrol);


        //Tool Tips
        ImageView movementToolTip = findViewById(R.id.infoMovement);

        movementToolTip.setOnClickListener(v -> {
            // Change tint to indicate click
            movementToolTip.setColorFilter(Color.parseColor("#80FFFFFF")); // 50% white tint

            // Create popup text
            TextView popupText = new TextView(this);
            popupText.setText("The D-Pad below provides direction of the rover.");
            popupText.setBackgroundColor(Color.parseColor("#CC000000"));
            popupText.setTextColor(Color.WHITE);
            popupText.setPadding(20, 10, 20, 10);
            popupText.setTextSize(12);

            PopupWindow popupWindow = new PopupWindow(popupText,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true);

            // Restore tint when popup dismisses
            popupWindow.setOnDismissListener(movementToolTip::clearColorFilter);

            // Show popup below the icon
            popupWindow.showAsDropDown(movementToolTip, 0, -movementToolTip.getHeight() / 8);
        });


        ImageView sprayInfo = findViewById(R.id.infoSpray);

        sprayInfo.setOnClickListener(v -> {
            sprayInfo.setColorFilter(Color.parseColor("#80FFFFFF"));

            TextView popupText = new TextView(this);
            popupText.setText("This button on hold begins to spray the paint, double tap for automatic.");
            popupText.setBackgroundColor(Color.parseColor("#CC000000"));
            popupText.setTextColor(Color.WHITE);
            popupText.setPadding(20, 10, 20, 10);
            popupText.setTextSize(12);

            PopupWindow popupWindow = new PopupWindow(popupText,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    true);

            popupWindow.setOnDismissListener(sprayInfo::clearColorFilter);
            popupWindow.showAsDropDown(sprayInfo, 0, -sprayInfo.getHeight() / 8);
        });


        //Back Button
        ImageView FooterBack = findViewById(R.id.footerBack);

        FooterBack.setOnClickListener(v -> {
            // Apply visual feedback tint
            FooterBack.setColorFilter(Color.parseColor("#80FFFFFF")); // semi-white highlight

            // Delay navigation slightly to show click feedback
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                FooterBack.clearColorFilter(); // remove tint before navigating
                Intent intent = new Intent(ManualControlActivity.this, DashboardActivity.class);
                startActivity(intent);
            }, 150); // short delay (150ms)
        });


        // Initialize ViewModel (shared Bluetooth logic)
        ConnectionViewModel viewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);


        viewModel.getConnectionStatus().observe(this, status -> {
            if (status == ConnectionStatus.DISCONNECTED) {
                updateBatteryUI(70);
                updateSprayUI(100);
            }
        });



        batteryIcon = findViewById(R.id.battery_icon);
        percentLevel = findViewById(R.id.percentLevel);

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

        percentLevel = findViewById(R.id.percentLevel);
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
