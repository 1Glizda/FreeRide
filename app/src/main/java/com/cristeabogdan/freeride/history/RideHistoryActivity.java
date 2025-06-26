package com.cristeabogdan.freeride.history;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.cristeabogdan.freeride.R;
import com.cristeabogdan.freeride.databinding.ActivityRideHistoryBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.ArrayList;

public class RideHistoryActivity extends AppCompatActivity {
    private static final String TAG = "RideHistoryActivity";

    private ActivityRideHistoryBinding binding;
    private RideHistoryAdapter adapter;
    private final ArrayList<RideHistoryItem> list = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRideHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = FirebaseFirestore.getInstance();
        adapter = new RideHistoryAdapter(list);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);

        binding.backButton.setOnClickListener(v -> finish());
        loadRides();
    }

    private void loadRides() {
        String uid = FirebaseAuth.getInstance().getUid();
        binding.progressBar.setVisibility(View.VISIBLE);
        Log.d(TAG, "Loading rides for user=" + uid);

        db.collection("rides")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(rv -> {
                    binding.progressBar.setVisibility(View.GONE);
                    list.clear();

                    if (rv.isEmpty()) {
                        binding.recyclerView.setVisibility(View.GONE);
                        binding.emptyStateLayout.setVisibility(View.VISIBLE);
                        return;
                    }

                    binding.emptyStateLayout.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);

                    // for each ride doc, kick off a payment fetch
                    for (DocumentSnapshot rideDoc : rv) {
                        RideHistoryItem item = new RideHistoryItem();
                        item.setRideId(   rideDoc.getId());
                        item.setDistance(rideDoc.getDouble("distance"));
                        item.setDuration(rideDoc.getDouble("duration"));
                        item.setTimestamp(
                                rideDoc.getTimestamp("timestamp").toDate().getTime()
                        );

                        list.add(item);
                    }
                    adapter.notifyDataSetChanged();

                    // now for each ride, load its payment to set the fare
                    for (int i = 0; i < list.size(); i++) {
                        final int idx = i;
                        String rideId = list.get(i).getRideId();

                        db.collection("payment")
                                .whereEqualTo("rideId", rideId)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(pr -> {
                                    if (!pr.isEmpty()) {
                                        Double fare = pr.getDocuments()
                                                .get(0)
                                                .getDouble("fare");
                                        list.get(idx).setFare(fare);
                                    } else {
                                        list.get(idx).setFare(0.0);
                                    }
                                    adapter.notifyItemChanged(idx);
                                })
                                .addOnFailureListener(e -> {
                                    Log.w(TAG, "payment lookup failed for " + rideId, e);
                                    list.get(idx).setFare(0.0);
                                    adapter.notifyItemChanged(idx);
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to load rides", e);
                    Toast.makeText(this, R.string.failed_load_rides, Toast.LENGTH_SHORT).show();
                    binding.recyclerView.setVisibility(View.GONE);
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                });
    }
}
