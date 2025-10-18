package com.example.fieldpainterbot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

// Activity responsible for displaying the selected field type and preparing the painting interface
public class PaintingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painting);

        // Retrieve the selected field type passed from FieldChoiceActivity
        String fieldType = getIntent().getStringExtra("fieldType");
        TextView title = findViewById(R.id.paintingTitle);

        if ("soccer".equals(fieldType)) {
            title.setText(getString(R.string.Label_Soccer));
            // TODO: Load soccer field layout
        } else if ("baseball".equals(fieldType)) {
            title.setText(getString(R.string.Label_Baseball));
            // TODO: Load baseball field layout
        }

        // Cancel button to return to field selection
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            // Create an intent to return to FieldChoiceActivity
            Intent intent = new Intent(PaintingActivity.this, FieldChoiceActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); //Finish this activity to prevent stacking
        });
    }
}
