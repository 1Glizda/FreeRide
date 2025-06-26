package com.cristeabogdan.fleetadmin;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.fleetadmin.database.FleetManager;
import com.cristeabogdan.fleetadmin.databinding.ActivityCarDetailsBinding;
import com.cristeabogdan.fleetadmin.models.Car;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CarDetailsActivity extends AppCompatActivity {

    private static final String TAG = "CarDetailsActivity";
    private ActivityCarDetailsBinding binding;
    private String carId;
    private Car currentCar;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCarDetailsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        carId = getIntent().getStringExtra("carId");
        if (carId == null) {
            Toast.makeText(this, "Invalid car ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI();
        loadCarDetails();
    }

    private void setupUI() {
        binding.backButton.setOnClickListener(v -> finish());
        binding.refreshButton.setOnClickListener(v -> loadCarDetails());
    }

    private void loadCarDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentLayout.setVisibility(View.GONE);
        
        FleetManager.getAllCars(new FleetManager.FleetCallback() {
            @Override
            public void onSuccess(List<Car> cars) {
                binding.progressBar.setVisibility(View.GONE);
                
                // Find the specific car
                Car foundCar = null;
                for (Car car : cars) {
                    if (carId.equals(car.getCarId())) {
                        foundCar = car;
                        break;
                    }
                }
                
                if (foundCar != null) {
                    currentCar = foundCar;
                    displayCarDetails(foundCar);
                    binding.contentLayout.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(CarDetailsActivity.this, "Car not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Exception e) {
                binding.progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Failed to load car details", e);
                Toast.makeText(CarDetailsActivity.this, "Failed to load car details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayCarDetails(Car car) {
        binding.titleTextView.setText("Car Details - " + car.getCarId());
        
        // Basic Information
        binding.carIdValueTextView.setText(car.getCarId());
        binding.carModelValueTextView.setText(car.getCarModel());
        binding.driverNameValueTextView.setText(car.getDriverName());
        binding.licensePlateValueTextView.setText(car.getLicensePlate());
        
        // Status Information
        binding.statusValueTextView.setText(car.getStatusText());
        int statusColor;
        if (car.isNeedsMaintenance()) {
            statusColor = getColor(R.color.maintenance_red);
        } else if (car.isAvailable()) {
            statusColor = getColor(R.color.available_green);
        } else {
            statusColor = getColor(R.color.in_use_orange);
        }
        binding.statusValueTextView.setTextColor(statusColor);
        
        // Location Information
        binding.latitudeValueTextView.setText(String.format(Locale.getDefault(), "%.6f", car.getLatitude()));
        binding.longitudeValueTextView.setText(String.format(Locale.getDefault(), "%.6f", car.getLongitude()));
        binding.h3IndexValueTextView.setText(car.getH3Index() != null ? car.getH3Index() : "N/A");
        
        // Timestamps
        if (car.getCreatedAt() != null) {
            binding.createdAtValueTextView.setText(dateFormat.format(new Date(car.getCreatedAt())));
        } else {
            binding.createdAtValueTextView.setText("N/A");
        }
        
        if (car.getLastUpdated() != null) {
            binding.lastUpdatedValueTextView.setText(dateFormat.format(new Date(car.getLastUpdated())));
        } else {
            binding.lastUpdatedValueTextView.setText("N/A");
        }
        
        // Maintenance Information
        if (car.isNeedsMaintenance()) {
            binding.maintenanceSection.setVisibility(View.VISIBLE);
            binding.maintenanceNotesValueTextView.setText(
                car.getMaintenanceNotes() != null ? car.getMaintenanceNotes() : "No notes available"
            );
        } else {
            binding.maintenanceSection.setVisibility(View.GONE);
        }
    }
}