package com.example.fieldpainterbot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

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




        //For manual control
        ClickableImageView ForwardBtn = findViewById(R.id.btnForward);
        ClickableImageView LeftBtn    = findViewById(R.id.btnLeft);
        ClickableImageView BackBtn    = findViewById(R.id.btnBack);
        ClickableImageView RightBtn   = findViewById(R.id.btnRight);
        ClickableImageView SprayBtn   = findViewById(R.id.sprayButton);

        // (Optional but fine)
        ForwardBtn.setClickable(true);
        LeftBtn.setClickable(true);
        BackBtn.setClickable(true);
        RightBtn.setClickable(true);
        SprayBtn.setClickable(true);

        // Required only to satisfy accessibility – empty click listener is fine
        ForwardBtn.setOnClickListener(v -> {});
        LeftBtn.setOnClickListener(v -> {});
        BackBtn.setOnClickListener(v -> {});
        RightBtn.setOnClickListener(v -> {});
        SprayBtn.setOnClickListener(v -> {});

        View.OnTouchListener controlTouch = (v, event) -> {

            // --- 1) Check Bluetooth connection FIRST ---
            if (viewModel.getConnectionStatus().getValue() != ConnectionStatus.CONNECTED) {
                Toast.makeText(this, "Not connected to robot", Toast.LENGTH_SHORT).show();
                return false; // ignore touch
            }

            // --- 2) Determine which command to send ---
            String cmd = "";

            if (v.getId() == R.id.btnForward) cmd = "FORWARD";
            if (v.getId() == R.id.btnBack)    cmd = "BACK";
            if (v.getId() == R.id.btnLeft)    cmd = "LEFT";
            if (v.getId() == R.id.btnRight)   cmd = "RIGHT";
            if (v.getId() == R.id.sprayButton) cmd = "SPRAY";

            if (cmd.isEmpty()) return false;


            // --- 3) Handle press + release events ---
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                viewModel.sendControlCommand(cmd, "pressed");
            }

            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {

                v.performClick(); // safe - custom view overrides performClick()
                viewModel.sendControlCommand(cmd, "released");
            }

            return true;
        };


        ForwardBtn.setOnTouchListener(controlTouch);
        LeftBtn.setOnTouchListener(controlTouch);
        BackBtn.setOnTouchListener(controlTouch);
        RightBtn.setOnTouchListener(controlTouch);
        SprayBtn.setOnTouchListener(controlTouch);



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
