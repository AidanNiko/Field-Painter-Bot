package com.example.fieldpainterbot;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PaintingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painting);


        String fieldType = getIntent().getStringExtra("fieldType");
        TextView title = findViewById(R.id.paintingTitle);

        if ("soccer".equals(fieldType)) {
            title.setText(getString(R.string.Label_Soccer));
            // TODO: Load soccer field layout
        } else if ("baseball".equals(fieldType)) {
            title.setText(getString(R.string.Label_Baseball));
            // TODO: Load baseball field layout
        }

        // Cancel button
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            Intent intent = new Intent(PaintingActivity.this, FieldChoiceActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }
}