package com.example.fieldpainterbot;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;


public class FieldChoiceActivity extends AppCompatActivity {

    private String selectedField = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_field_choice);
        
        Log.d("FieldChoiceActivity", "FieldChoice Activity created");

        // set up click handling for cards
        setupCardClicks();

        MaterialButton startbutton = findViewById(R.id.BeginProcess);
        ConnectionViewModel viewModel = ConnectionViewModel.getInstance(getApplication());
        
        ConnectionStatus initialStatus = viewModel.getConnectionStatus().getValue();
        Log.d("FieldChoiceActivity", "Initial connection status on FieldChoice: " + initialStatus);
        
        viewModel.getConnectionStatus().observe(this, status -> {
            Log.d("FieldChoiceActivity", "Connection status observer triggered - New status: " + status);
        });

        startbutton.setOnClickListener(v -> {
            Log.d("FieldChoiceActivity", "Start button clicked");
            
            startbutton.setEnabled(false);
            startbutton.setText("Sending");

            ConnectionStatus status = viewModel.getConnectionStatus().getValue();
            Log.d("FieldChoiceActivity", "Button click - Checking connection status: " + status);
            Log.d("FieldChoiceActivity", "Selected field: " + selectedField);

            // ⚠️ If NOT connected → fetch data and send to backend
            if (status != ConnectionStatus.CONNECTED) {
                Log.d("FieldChoiceActivity", "NOT connected (status=" + status + ") - Button returning early, not proceeding");
                Intent intent = new Intent(FieldChoiceActivity.this, PaintingActivity.class);
                startActivity(intent);
                return;
            }

            // ✅ Connected → send normally
            Log.d("FieldChoiceActivity", "Connected (status=" + status + ") - Proceeding with fetchFieldAndSend");
            viewModel.fetchFieldAndSend(
                    selectedField,
                    () -> {
                        Log.d("FieldChoiceActivity", "fetchFieldAndSend success callback - Navigating to PaintingActivity");
                        Intent intent = new Intent(FieldChoiceActivity.this, PaintingActivity.class);
                        startActivity(intent);
                    },
                    () -> {
                        Log.d("FieldChoiceActivity", "fetchFieldAndSend error callback - Re-enabling button");
                        startbutton.setEnabled(true);
                        startbutton.setText("Start");
                    }
            );
        });

        // back button to go back to dashboard
        MaterialButton backbutton = findViewById(R.id.BackDshB);
        backbutton.setOnClickListener(v -> {
            Intent intent = new Intent(FieldChoiceActivity.this, DashboardActivity.class);
            startActivity(intent);
        });
    }



    // keeps track of which card is selected
    private MaterialCardView selectedCard = null;

    // sets up all card click listeners
    private void setupCardClicks() {
        int[] cardIds = {
                R.id.card_soccer,
                R.id.card_baseball,
                R.id.card_tennis,
                R.id.card_basketball,
                R.id.card_hockey,
                R.id.card_rugby
        };

        for (int id : cardIds) {
            MaterialCardView card = findViewById(id);
            card.setOnClickListener(v -> handleCardClick((MaterialCardView) v));
        }
    }

    // handles color + sets string value
    private void handleCardClick(MaterialCardView clickedCard) {
        if (clickedCard == null) return;

        Log.d("FieldChoiceActivity", "Card clicked with ID: " + clickedCard.getId());

        // Reset previously selected card (if different)
        if (selectedCard != null && selectedCard != clickedCard) {
            selectedCard.setStrokeColor(Color.parseColor("#FFFFFF")); // white border reset
        }

        // Highlight the clicked one
        clickedCard.setStrokeColor(Color.parseColor("#2196F3")); // blue border highlight

        // Update the selected card reference
        selectedCard = clickedCard;

        // Update selected field string
        int id = clickedCard.getId();
        if (id == R.id.card_soccer) {
            selectedField = "soccer";
        } else if (id == R.id.card_baseball) {
            selectedField = "baseball";
        } else if (id == R.id.card_tennis) {
            selectedField = "tennis";
        } else if (id == R.id.card_basketball) {
            selectedField = "basketball";
        } else if (id == R.id.card_hockey) {
            selectedField = "hockey";
        } else if (id == R.id.card_rugby) {
            selectedField = "rugby";
        } else {
            selectedField = "";
        }
        Log.d("FieldChoiceActivity", "Field selected: " + selectedField);
    }


}