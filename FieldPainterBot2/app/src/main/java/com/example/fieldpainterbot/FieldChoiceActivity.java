package com.example.fieldpainterbot;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class FieldChoiceActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_choice);

        findViewById(R.id.soccerFieldCard).setOnClickListener(v -> {
            Intent intent = new Intent(this, PaintingActivity.class);
            intent.putExtra("fieldType", "soccer");
            startActivity(intent);
        });

        findViewById(R.id.baseballFieldCard).setOnClickListener(v -> {
            Intent intent = new Intent(this, PaintingActivity.class);
            intent.putExtra("fieldType", "baseball");
            startActivity(intent);
        });
    }
}