package com.example.fieldpainterbot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

public class DashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);


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

        //Find the clickable bluetooth logo
        ImageView bluetoothicon = findViewById(R.id.bluetooth_icon);

        //Bluetooth icon navigates back the device connectivity page
        bluetoothicon.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ConnectionActivity.class);
            startActivity(intent);
        });

        MaterialCardView card1 = findViewById(R.id.header_clickable_1);
        MaterialCardView card2 = findViewById(R.id.header_clickable_2);

        card1.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, FieldChoiceActivity.class);
            startActivity(intent);
        });
        card2.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ManualControlActivity.class);
            startActivity(intent);
        });


    }
}



