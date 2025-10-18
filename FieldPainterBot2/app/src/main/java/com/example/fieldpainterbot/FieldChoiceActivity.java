package com.example.fieldpainterbot;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

//Activity that allows the user to choose a type of sports field to paint.
// Navigates to PaintingActivity with the selected field type
public class FieldChoiceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_choice);

        // Set up click listener for the soccer field card
        findViewById(R.id.soccerFieldCard).setOnClickListener(v -> {
            // Create an intent to launch PaintingActivity
            Intent intent = new Intent(this, PaintingActivity.class);
            // Pass the selected field type as an extra
            intent.putExtra("fieldType", "soccer");
            // Start the painting activity
            startActivity(intent);
        });
        // Set up click listener for the baseball field card
        findViewById(R.id.baseballFieldCard).setOnClickListener(v -> {
            Intent intent = new Intent(this, PaintingActivity.class);
            intent.putExtra("fieldType", "baseball");
            startActivity(intent);
        });
    }
}
