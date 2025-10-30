package com.example.fieldpainterbot;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;


public class ManualControlActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manualcontrol);


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



    }
}
