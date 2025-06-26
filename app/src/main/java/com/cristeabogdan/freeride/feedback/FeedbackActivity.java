package com.cristeabogdan.freeride.feedback;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.cristeabogdan.freeride.databinding.ActivityFeedbackBinding;
import com.cristeabogdan.freeride.maps.MapsActivity;
import com.cristeabogdan.freeride.payment.PaymentActivity;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class FeedbackActivity extends AppCompatActivity {
    private static final String TAG = "FeedbackActivity";
    private ActivityFeedbackBinding binding;
    private FirebaseFirestore db;
    private int selectedRating = 0;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        binding = ActivityFeedbackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();

        Intent i = getIntent();

        binding.ratingBar.setOnRatingBarChangeListener(
                (rb, r, fromUser) -> { if (fromUser) selectedRating = (int) r; }
        );

        binding.submitButton.setOnClickListener(v -> submitFeedback());
        binding.skipButton  .setOnClickListener(v -> navigateToMaps());
        binding.tripDistanceTextView.setText(PaymentActivity.KM + " KM");
        binding.tripDurationTextView.setText(PaymentActivity.MINUTES + " Minutes");
        binding.tripAmountTextView.setText("$" + PaymentActivity.totalFare);
    }

    private void submitFeedback() {
        String comment = binding.feedbackEditText.getText().toString().trim();
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.progressBar.setVisibility(android.view.View.VISIBLE);
        binding.submitButton.setEnabled(false);
        saveFeedback(selectedRating, comment);
    }

    private void saveFeedback(int rating, String comment) {
        // pull rideId from SharedPreferences
        SharedPreferences prefs = getSharedPreferences("RidePrefs", MODE_PRIVATE);
        String rideId = prefs.getString("last_ride_id", null);
        if (rideId == null) {
            Toast.makeText(this, "No ride ID found, feedback not saved.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        Map<String,Object> fb = new HashMap<>();
        fb.put("rideId",       rideId);
        fb.put("rating",       rating);
        fb.put("comment",      comment);
        fb.put("timestamp",    FieldValue.serverTimestamp());

        db.collection("feedback")
                .add(fb)
                .addOnSuccessListener(docRef -> {
                    Log.d(TAG, "Feedback saved: " + docRef.getId());
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.submitButton.setEnabled(true);
                    Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
                    navigateToMaps();
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error saving feedback", e);
                    binding.progressBar.setVisibility(android.view.View.GONE);
                    binding.submitButton.setEnabled(true);
                    Toast.makeText(this, "Failed to submit feedback. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToMaps() {
        startActivity(new Intent(this, MapsActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK)
        );
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        navigateToMaps();
    }
}
