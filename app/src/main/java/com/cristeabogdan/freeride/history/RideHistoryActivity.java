package com.cristeabogdan.freeride.history;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cristeabogdan.freeride.databinding.ActivityRideHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RideHistoryActivity extends AppCompatActivity {

    private static final String TAG = "RideHistoryActivity";
    private ActivityRideHistoryBinding binding;
    private FirebaseFirestore db;
    private RideHistoryAdapter adapter;
    private List<RideHistoryItem> rideHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRideHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
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

        // Load feedback data as ride history (since we don't have a separate rides collection)
        db.collection("feedback")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    rideHistoryList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> data = document.getData();
                        
                        RideHistoryItem item = new RideHistoryItem();
                        item.setId(document.getId());
                        item.setAmount((Double) data.get("tripAmount"));
                        item.setDistance((String) data.get("tripDistance"));
                        item.setDuration((String) data.get("tripDuration"));
                        item.setRating((Long) data.get("rating"));
                        item.setTimestamp((Long) data.get("timestamp"));
                        
                        rideHistoryList.add(item);
                    }

                    if (rideHistoryList.isEmpty()) {
                        showEmptyState();
                    } else {
                        binding.emptyStateLayout.setVisibility(View.GONE);
                        binding.recyclerView.setVisibility(View.VISIBLE);
                        adapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading ride history", e);
                    Toast.makeText(this, "Failed to load ride history", Toast.LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    private void showEmptyState() {
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyStateLayout.setVisibility(View.VISIBLE);
    }
}