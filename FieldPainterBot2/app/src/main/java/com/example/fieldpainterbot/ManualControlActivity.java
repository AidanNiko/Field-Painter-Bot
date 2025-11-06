package com.example.fieldpainterbot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class ManualControlActivity extends AppCompatActivity {
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


        //Will change once db class is made with a get method
        int batteryLevel = 70;
        ImageView batteryIcon = findViewById(R.id.battery_icon);

        //Set the text for battery level
        TextView percentLevel = findViewById(R.id.percentLevel);
        percentLevel.setText(getString(R.string.battery_percent, batteryLevel));

        //Determine what the image should look like depending on battery level
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

        //Will change once db class is made with a get method
        int sprayPercentLevel = 75;
        ImageView sprayIcon = findViewById(R.id.spray_icon);

        //Set the text for battery level
        TextView sprayLevel = findViewById(R.id.sprayLevel);
        sprayLevel.setText(getString(R.string.battery_percent, sprayPercentLevel));

        //Determine what the image should look like depending on battery level
        if (sprayPercentLevel >= 80) {
            sprayIcon.setImageResource(R.drawable.spray_100);
        } else if (sprayPercentLevel >= 60) {
            sprayIcon.setImageResource(R.drawable.spray_75);
        } else if (sprayPercentLevel >= 40) {
            sprayIcon.setImageResource(R.drawable.spray_50);
        } else if (sprayPercentLevel >= 20) {
            sprayIcon.setImageResource(R.drawable.spray_25);
        } else {
            sprayIcon.setImageResource(R.drawable.spray_0);
        }



    }
}
