package com.example.fieldpainterbot;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;



public class PaintingActivity extends AppCompatActivity {
    private TextView percentLevel;

    private TextView percentSpray;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painting);


        percentLevel = findViewById(R.id.batteryStatus);
        percentSpray = findViewById(R.id.paintStatus);
        ProgressBar progress = findViewById(R.id.progressBar);
        TextView Connection = findViewById(R.id.connectionStatus);
        ImageView loadingAnimation = findViewById(R.id.loadingAnimation);
        AnimationDrawable anim = (AnimationDrawable) loadingAnimation.getDrawable(); // or getBackground()
        anim.start();

        ConnectionViewModel viewModel = new ViewModelProvider(this).get(ConnectionViewModel.class);

        viewModel.getConnectionStatus().observe(this, status -> {
            if (status == ConnectionStatus.DISCONNECTED) {
                updateBatteryUI(70);
                updateSprayUI(100);
                progress.setProgress(70);
                Connection.setText("Connection Status: Disconnected");
            }
            else{
                Connection.setText("Connection Status: Connected");
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

        Button actionButton = findViewById(R.id.cancelButton);

// Default behavior (Cancel → Dashboard)
        Runnable defaultAction = () -> {
            Intent intent = new Intent(PaintingActivity.this, DashboardActivity.class);
            startActivity(intent);
        };

// Set initial behavior
        actionButton.setOnClickListener(v -> defaultAction.run());

        viewModel.getProgressLevel().observe(this, level -> {
            if (level != null) {
                progress.setProgress(level);
                Log.d("Progress", "Progress updated: " + level);

                if (level >= 100) {
                    // Show DONE state
                    actionButton.setText("Done");

                    // Change action to navigate to Finish page
                    actionButton.setOnClickListener(v -> {
                        Intent intent = new Intent(PaintingActivity.this, DashboardActivity.class);
                        startActivity(intent);
                    });

                } else {
                    // Restore CANCEL state
                    actionButton.setText("Cancel");
                    actionButton.setOnClickListener(v -> defaultAction.run());
                }
            }
            else {
                Log.d("Bluetooth", "Disconnected");
                progress.setProgress(70);
            }
        });

    }
    private void updateBatteryUI(int batteryLevel) {
        percentLevel.setText("Battery Remaining: "+batteryLevel + "%");
    }

    private void updateSprayUI(int sprayLevel) {
        percentSpray.setText("Paint Remaining: "+sprayLevel + "%");
    }

}