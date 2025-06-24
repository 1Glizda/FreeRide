package com.cristeabogdan.freeride.payment;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.databinding.ActivityPaymentBinding;
import com.cristeabogdan.freeride.feedback.FeedbackActivity;
import com.stripe.android.PaymentConfiguration;
import com.stripe.android.paymentsheet.PaymentSheet;
import com.stripe.android.paymentsheet.PaymentSheetResult;

import java.text.DecimalFormat;
import java.util.Random;

public class PaymentActivity extends AppCompatActivity {

    private static final String TAG = "PaymentActivity";
    private static final String STRIPE_PUBLISHABLE_KEY = "pk_test_51RcpxFJxNlsGSyYX3MSgdfJEwgK3Z4nMYkeRuqjtmMB4KUFQuM8Jl3kzAvjTqZ1JNMfDqN1bjFAfU3Q7VYuTPiaf00I5IUySuG";

    private ActivityPaymentBinding binding;
    private PaymentSheet paymentSheet;
    private double tripAmount;
    private String tripDistance;
    private String tripDuration;
    private String rideId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPaymentBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Stripe
        PaymentConfiguration.init(getApplicationContext(), STRIPE_PUBLISHABLE_KEY);
        paymentSheet = new PaymentSheet(this, this::onPaymentSheetResult);

        // Get trip data from intent
        Intent intent = getIntent();
        rideId = intent.getStringExtra("ride_id");
        
        generateTripData();
        setupUI();
        setUpClickListeners();
    }

    private void generateTripData() {
        // In a real app, this data would come from trip tracking
        Random random = new Random();
        tripAmount = 10.50 + (random.nextDouble() * 20); // $10.50 - $30.50
        tripDistance = String.format("%.1f km", 2.0 + (random.nextDouble() * 8)); // 2-10 km
        tripDuration = String.format("%d min", 15 + random.nextInt(25)); // 15-40 min
    }

    private void setupUI() {
        DecimalFormat df = new DecimalFormat("#.##");

        binding.tripAmountTextView.setText("$" + df.format(tripAmount));
        binding.tripDistanceTextView.setText(tripDistance);
        binding.tripDurationTextView.setText(tripDuration);

        // Calculate breakdown
        double baseFare = 3.00;
        double distanceFare = tripAmount * 0.6;
        double timeFare = tripAmount * 0.2;
        double serviceFee = tripAmount * 0.1;
        double tax = tripAmount * 0.1;

        binding.baseFareAmountTextView.setText("$" + df.format(baseFare));
        binding.distanceFareAmountTextView.setText("$" + df.format(distanceFare));
        binding.timeFareAmountTextView.setText("$" + df.format(timeFare));
        binding.serviceFeeAmountTextView.setText("$" + df.format(serviceFee));
        binding.taxAmountTextView.setText("$" + df.format(tax));
        binding.totalAmountTextView.setText("$" + df.format(tripAmount));
    }

    private void setUpClickListeners() {
        binding.payButton.setOnClickListener(v -> processPayment());

        binding.backButton.setOnClickListener(v -> {
            // In a real app, you might want to handle this differently
            Toast.makeText(this, "Payment is required to complete the trip", Toast.LENGTH_SHORT).show();
        });
    }

    private void processPayment() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.payButton.setEnabled(false);

        // In a real implementation, you would:
        // 1. Call your backend to create a payment intent
        // 2. Pass the client secret to Stripe PaymentSheet
        // For demo purposes, we'll simulate a successful payment

        // Simulate network delay
        binding.getRoot().postDelayed(() -> {
            // Simulate successful payment
            simulateSuccessfulPayment();
        }, 2000);
    }

    private void simulateSuccessfulPayment() {
        binding.progressBar.setVisibility(View.GONE);
        binding.payButton.setEnabled(true);

        // Show success message
        Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();

        // Navigate to feedback
        Intent intent = new Intent(this, FeedbackActivity.class);
        intent.putExtra("trip_amount", tripAmount);
        intent.putExtra("trip_distance", tripDistance);
        intent.putExtra("trip_duration", tripDuration);
        intent.putExtra("ride_id", rideId);
        startActivity(intent);
        finish();
    }

    private void onPaymentSheetResult(PaymentSheetResult paymentSheetResult) {
        binding.progressBar.setVisibility(View.GONE);
        binding.payButton.setEnabled(true);

        if (paymentSheetResult instanceof PaymentSheetResult.Completed) {
            Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(this, FeedbackActivity.class);
            intent.putExtra("trip_amount", tripAmount);
            intent.putExtra("trip_distance", tripDistance);
            intent.putExtra("trip_duration", tripDuration);
            intent.putExtra("ride_id", rideId);
            startActivity(intent);
            finish();
        } else if (paymentSheetResult instanceof PaymentSheetResult.Canceled) {
            Log.i(TAG, "Payment canceled by user");
        } else if (paymentSheetResult instanceof PaymentSheetResult.Failed) {
            PaymentSheetResult.Failed failed = (PaymentSheetResult.Failed) paymentSheetResult;
            Log.e(TAG, "Payment failed", failed.getError());
            Toast.makeText(this, "Payment failed: " + failed.getError().getLocalizedMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }
}