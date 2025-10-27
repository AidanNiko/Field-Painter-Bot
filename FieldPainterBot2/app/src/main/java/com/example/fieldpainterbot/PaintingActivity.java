package com.example.fieldpainterbot;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PaintingActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_painting);

        ImageView loadingAnimation = findViewById(R.id.loadingAnimation);
        AnimationDrawable anim = (AnimationDrawable) loadingAnimation.getDrawable(); // or getBackground()
        anim.start();

        // Cancel button
        Button cancelButton = findViewById(R.id.cancelButton);
        cancelButton.setOnClickListener(v -> {
            Intent intent = new Intent(PaintingActivity.this, DashboardActivity.class);
            startActivity(intent);
        });
    }
}