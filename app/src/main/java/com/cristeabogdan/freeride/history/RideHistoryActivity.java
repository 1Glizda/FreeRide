package com.cristeabogdan.freeride.history;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cristeabogdan.freeride.databinding.ActivityRideHistoryBinding;
import com.cristeabogdan.freeride.database.Ride;
import com.cristeabogdan.freeride.database.RideManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class RideHistoryActivity extends AppCompatActivity {

    private static final String TAG = "RideHistoryActivity";
    private ActivityRideHistoryBinding binding;
    private RideHistoryAdapter adapter;
    private List<RideHistoryItem> rideHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRideHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        rideHistoryList = new ArrayList<>();

        setupRecyclerView();
        setUpClickListeners();
        loadRideHistory();
    }

    private void setupRecyclerView() {
        adapter = new RideHistoryAdapter(rideHistoryList);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setUpClickListeners() {
        binding.backButton.setOnClickListener(v -> finish());
    }

    private void loadRideHistory() {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;

        if (userId == null) {
            showEmptyState();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);

        RideManager.getUserRides(userId, new RideManager.RideQueryCallback() {
            @Override
            public void onSuccess(List<Ride> rides) {
                binding.progressBar.setVisibility(View.GONE);
                rideHistoryList.clear();

                for (Ride ride : rides) {
                    // Only show completed rides
                    if ("TRIP_COMPLETED".equals(ride.getStatus())) {
                        RideHistoryItem item = new RideHistoryItem();
                        item.setId(ride.getId());
                        item.setCarId(ride.getCarId());
                        item.setAmount(ride.getAmount());
                        item.setDistance(ride.getDistance());
                        item.setDuration(ride.getDuration());
                        item.setRating(ride.getRating());
                        item.setTimestamp(ride.getTripCompletedAt() != null ? 
                            ride.getTripCompletedAt() : ride.getCreatedAt());
                        
                        rideHistoryList.add(item);
                    }
                }

                if (rideHistoryList.isEmpty()) {
                    showEmptyState();
                } else {
                    binding.emptyStateLayout.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading ride history", e);
                Toast.makeText(RideHistoryActivity.this, "Failed to load ride history", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void showEmptyState() {
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
    }
}