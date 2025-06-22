package com.cristeabogdan.freeride.feedback;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.databinding.ActivityFeedbackBinding;
import com.cristeabogdan.freeride.maps.MapsActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {

    private static final String TAG = "FeedbackActivity";
    private ActivityFeedbackBinding binding;
    private FirebaseFirestore db;
    private double tripAmount;
    private String tripDistance;
    private String tripDuration;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        // Get trip data from intent
        Intent intent = getIntent();
        tripAmount = intent.getDoubleExtra("trip_amount", 0.0);
        tripDistance = intent.getStringExtra("trip_distance");
        tripDuration = intent.getStringExtra("trip_duration");

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

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.submitButton.setEnabled(false);

        // Save feedback to Firestore
        saveFeedbackToFirestore(selectedRating, comment);
    }

    private void saveFeedbackToFirestore(int rating, String comment) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "anonymous";

        Map<String, Object> feedback = new HashMap<>();
        feedback.put("userId", userId);
        feedback.put("rating", rating);
        feedback.put("comment", comment);
        feedback.put("tripAmount", tripAmount);
        feedback.put("tripDistance", tripDistance);
        feedback.put("tripDuration", tripDuration);
        feedback.put("timestamp", System.currentTimeMillis());

        db.collection("feedback")
                .add(feedback)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Feedback saved with ID: " + documentReference.getId());
                    binding.progressBar.setVisibility(View.GONE);
                    binding.submitButton.setEnabled(true);

                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    navigateToMaps();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding feedback", e);
                    binding.progressBar.setVisibility(View.GONE);
                    binding.submitButton.setEnabled(true);

                    Toast.makeText(this, "Failed to submit feedback. Please try again.",
                            Toast.LENGTH_SHORT).show();
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