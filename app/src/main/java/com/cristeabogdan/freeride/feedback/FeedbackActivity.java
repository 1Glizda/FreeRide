package com.cristeabogdan.freeride.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.databinding.ActivityFeedbackBinding;
import com.cristeabogdan.freeride.database.RideManager;
import com.cristeabogdan.freeride.maps.MapsActivity;

import java.text.DecimalFormat;

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackActivity";
    private ActivityFeedbackBinding binding;
    private double tripAmount;
    private String tripDistance;
    private String tripDuration;
    private String rideId;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get trip data from intent
        Intent intent = getIntent();
        tripAmount = intent.getDoubleExtra("trip_amount", 0.0);
        tripDistance = intent.getStringExtra("trip_distance");
        tripDuration = intent.getStringExtra("trip_duration");
        rideId = intent.getStringExtra("ride_id");

        setupUI();
        setUpClickListeners();
    }

    private void setupUI() {
        DecimalFormat df = new DecimalFormat("#.##");

        binding.tripSummaryTextView.setText(
                String.format("Trip completed • %s • %s • $%s",
                        tripDistance, tripDuration, df.format(tripAmount))
        );
    }

    private void setUpClickListeners() {
        // Rating stars
        binding.star1.setOnClickListener(v -> setRating(1));
        binding.star2.setOnClickListener(v -> setRating(2));
        binding.star3.setOnClickListener(v -> setRating(3));
        binding.star4.setOnClickListener(v -> setRating(4));
        binding.star5.setOnClickListener(v -> setRating(5));

        // Submit feedback
        binding.submitButton.setOnClickListener(v -> submitFeedback());

        // Skip feedback
        binding.skipButton.setOnClickListener(v -> navigateToMaps());
    }

    private void setRating(int rating) {
        selectedRating = rating;
        updateStarDisplay();
    }

    private void updateStarDisplay() {
        binding.star1.setSelected(selectedRating >= 1);
        binding.star2.setSelected(selectedRating >= 2);
        binding.star3.setSelected(selectedRating >= 3);
        binding.star4.setSelected(selectedRating >= 4);
        binding.star5.setSelected(selectedRating >= 5);
    }

    private void submitFeedback() {
        String comment = binding.feedbackEditText.getText().toString().trim();

        // Validate input
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        if (rideId == null) {
            Toast.makeText(this, "No ride ID found", Toast.LENGTH_SHORT).show();
            navigateToMaps();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.submitButton.setEnabled(false);

        // Save feedback to Firestore
        RideManager.saveFeedback(rideId, selectedRating, comment, new RideManager.RideUpdateCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Feedback saved successfully");
                binding.progressBar.setVisibility(View.GONE);
                binding.submitButton.setEnabled(true);

                Toast.makeText(FeedbackActivity.this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                navigateToMaps();
            }

            @Override
            public void onFailure(Exception e) {
                Log.w(TAG, "Error saving feedback", e);
                binding.progressBar.setVisibility(View.GONE);
                binding.submitButton.setEnabled(true);

                Toast.makeText(FeedbackActivity.this, "Failed to submit feedback. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToMaps() {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to payment
        navigateToMaps();
    }
}