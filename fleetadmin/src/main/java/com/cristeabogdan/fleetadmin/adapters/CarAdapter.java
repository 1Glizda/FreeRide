package com.cristeabogdan.fleetadmin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.cristeabogdan.fleetadmin.R;
import com.cristeabogdan.fleetadmin.models.Car;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CarAdapter extends RecyclerView.Adapter<CarAdapter.CarViewHolder> {

    private List<Car> cars;
    private OnCarClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    public interface OnCarClickListener {
        void onCarClick(Car car);
        void onMaintenanceClick(Car car);
    }

    public CarAdapter(List<Car> cars, OnCarClickListener listener) {
        this.cars = cars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_car, parent, false);
        return new CarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarViewHolder holder, int position) {
        Car car = cars.get(position);
        
        holder.carIdTextView.setText(car.getCarId());
        holder.carModelTextView.setText(car.getCarModel());
        holder.driverNameTextView.setText(car.getDriverName());
        holder.licensePlateTextView.setText(car.getLicensePlate());
        holder.statusTextView.setText(car.getStatusText());
        holder.locationTextView.setText(car.getLocationText());
        
        // Set status color
        int statusColor;
        if (car.isNeedsMaintenance()) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.maintenance_red);
        } else if (car.isAvailable()) {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.available_green);
        } else {
            statusColor = ContextCompat.getColor(holder.itemView.getContext(), R.color.in_use_orange);
        }
        holder.statusTextView.setTextColor(statusColor);
        
        // Show last updated time if available
        if (car.getLastUpdated() != null) {
            String lastUpdated = "Updated: " + dateFormat.format(new Date(car.getLastUpdated()));
            holder.lastUpdatedTextView.setText(lastUpdated);
            holder.lastUpdatedTextView.setVisibility(View.VISIBLE);
        } else {
            holder.lastUpdatedTextView.setVisibility(View.GONE);
        }
        
        // Show maintenance notes if available
        if (car.isNeedsMaintenance() && car.getMaintenanceNotes() != null) {
            holder.maintenanceNotesTextView.setText("Notes: " + car.getMaintenanceNotes());
            holder.maintenanceNotesTextView.setVisibility(View.VISIBLE);
        } else {
            holder.maintenanceNotesTextView.setVisibility(View.GONE);
        }
        
        // Set click listeners
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCarClick(car);
            }
        });
        
        holder.maintenanceButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMaintenanceClick(car);
            }
        });
        
        // Show/hide maintenance button based on status
        if (car.isNeedsMaintenance()) {
            holder.maintenanceButton.setText("Complete Maintenance");
            holder.maintenanceButton.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.available_green)
            );
        } else {
            holder.maintenanceButton.setText("Schedule Maintenance");
            holder.maintenanceButton.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.maintenance_red)
            );
        }
    }

    @Override
    public int getItemCount() {
        return cars.size();
    }

    public void updateCars(List<Car> newCars) {
        this.cars = newCars;
        notifyDataSetChanged();
    }

    static class CarViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView carIdTextView;
        TextView carModelTextView;
        TextView driverNameTextView;
        TextView licensePlateTextView;
        TextView statusTextView;
        TextView locationTextView;
        TextView lastUpdatedTextView;
        TextView maintenanceNotesTextView;
        TextView maintenanceButton;

        CarViewHolder(View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            carIdTextView = itemView.findViewById(R.id.carIdTextView);
            carModelTextView = itemView.findViewById(R.id.carModelTextView);
            driverNameTextView = itemView.findViewById(R.id.driverNameTextView);
            licensePlateTextView = itemView.findViewById(R.id.licensePlateTextView);
            statusTextView = itemView.findViewById(R.id.statusTextView);
            locationTextView = itemView.findViewById(R.id.locationTextView);
            lastUpdatedTextView = itemView.findViewById(R.id.lastUpdatedTextView);
            maintenanceNotesTextView = itemView.findViewById(R.id.maintenanceNotesTextView);
            maintenanceButton = itemView.findViewById(R.id.maintenanceButton);
        }
    }
}