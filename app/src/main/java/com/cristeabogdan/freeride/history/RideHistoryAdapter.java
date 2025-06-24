package com.cristeabogdan.freeride.history;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.cristeabogdan.freeride.R;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RideHistoryAdapter extends RecyclerView.Adapter<RideHistoryAdapter.ViewHolder> {

    private List<RideHistoryItem> rideHistoryList;
    private DecimalFormat decimalFormat = new DecimalFormat("#.##");
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault());

    public RideHistoryAdapter(List<RideHistoryItem> rideHistoryList) {
        this.rideHistoryList = rideHistoryList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ride_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RideHistoryItem item = rideHistoryList.get(position);

        // Format amount
        String amount = item.getAmount() != null ? "$" + decimalFormat.format(item.getAmount()) : "$0.00";
        holder.amountTextView.setText(amount);

        // Set trip details with car ID
        String tripDetails = String.format("%s • %s • %s", 
            item.getDistance() != null ? item.getDistance() : "0 km",
            item.getDuration() != null ? item.getDuration() : "0 min",
            item.getCarId() != null ? item.getCarId() : "Unknown Car");
        holder.tripDetailsTextView.setText(tripDetails);

        // Format date
        if (item.getTimestamp() != null) {
            Date date = new Date(item.getTimestamp());
            holder.dateTextView.setText(dateFormat.format(date));
        } else {
            holder.dateTextView.setText("Unknown date");
        }

        // Set rating stars
        int rating = item.getRating() != null ? item.getRating().intValue() : 0;
        StringBuilder stars = new StringBuilder();
        for (int i = 1; i <= 5; i++) {
            if (i <= rating) {
                stars.append("★");
            } else {
                stars.append("☆");
            }
        }
        holder.ratingTextView.setText(stars.toString());
    }

    @Override
    public int getItemCount() {
        return rideHistoryList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView amountTextView;
        TextView tripDetailsTextView;
        TextView dateTextView;
        TextView ratingTextView;

        ViewHolder(View itemView) {
            super(itemView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            tripDetailsTextView = itemView.findViewById(R.id.tripDetailsTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
        }
    }
}