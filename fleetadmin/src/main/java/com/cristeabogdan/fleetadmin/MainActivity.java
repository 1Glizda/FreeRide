package com.cristeabogdan.fleetadmin;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.cristeabogdan.fleetadmin.adapters.CarAdapter;
import com.cristeabogdan.fleetadmin.database.FleetManager;
import com.cristeabogdan.fleetadmin.databinding.ActivityMainBinding;
import com.cristeabogdan.fleetadmin.models.Car;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements CarAdapter.OnCarClickListener {

    private static final String TAG = "MainActivity";
    private ActivityMainBinding binding;
    private CarAdapter carAdapter;
    private List<Car> allCars = new ArrayList<>();
    private String currentFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupRecyclerView();
        setupFilterButtons();
        setupSwipeRefresh();
        loadCars();
    }

    private void setupRecyclerView() {
        carAdapter = new CarAdapter(allCars, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(carAdapter);
    }

    private void setupFilterButtons() {
        binding.filterAllButton.setOnClickListener(v -> filterCars("ALL"));
        binding.filterAvailableButton.setOnClickListener(v -> filterCars("AVAILABLE"));
        binding.filterInUseButton.setOnClickListener(v -> filterCars("IN_USE"));
        binding.filterMaintenanceButton.setOnClickListener(v -> filterCars("MAINTENANCE"));
        
        // Set initial filter
        updateFilterButtonStates("ALL");
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener(this::loadCars);
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.colorPrimary,
            R.color.colorAccent
        );
    }

    private void loadCars() {
        binding.swipeRefreshLayout.setRefreshing(true);
        binding.progressBar.setVisibility(View.VISIBLE);
        
        FleetManager.getAllCars(new FleetManager.FleetCallback() {
            @Override
            public void onSuccess(List<Car> cars) {
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.progressBar.setVisibility(View.GONE);
                
                allCars.clear();
                allCars.addAll(cars);
                
                updateStats();
                filterCars(currentFilter);
                
                if (cars.isEmpty()) {
                    binding.emptyStateLayout.setVisibility(View.VISIBLE);
                    binding.recyclerView.setVisibility(View.GONE);
                } else {
                    binding.emptyStateLayout.setVisibility(View.GONE);
                    binding.recyclerView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Exception e) {
                binding.swipeRefreshLayout.setRefreshing(false);
                binding.progressBar.setVisibility(View.GONE);
                
                Log.e(TAG, "Failed to load cars", e);
                Toast.makeText(MainActivity.this, "Failed to load fleet data", Toast.LENGTH_SHORT).show();
                
                binding.emptyStateLayout.setVisibility(View.VISIBLE);
                binding.recyclerView.setVisibility(View.GONE);
            }
        });
    }

    private void updateStats() {
        int totalCars = allCars.size();
        int availableCars = 0;
        int inUseCars = 0;
        int maintenanceCars = 0;
        
        for (Car car : allCars) {
            if (car.isNeedsMaintenance()) {
                maintenanceCars++;
            } else if (car.isAvailable()) {
                availableCars++;
            } else {
                inUseCars++;
            }
        }
        
        binding.totalCarsTextView.setText(String.valueOf(totalCars));
        binding.availableCarsTextView.setText(String.valueOf(availableCars));
        binding.inUseCarsTextView.setText(String.valueOf(inUseCars));
        binding.maintenanceCarsTextView.setText(String.valueOf(maintenanceCars));
    }

    private void filterCars(String filter) {
        currentFilter = filter;
        updateFilterButtonStates(filter);
        
        List<Car> filteredCars = new ArrayList<>();
        
        switch (filter) {
            case "ALL":
                filteredCars.addAll(allCars);
                break;
            case "AVAILABLE":
                for (Car car : allCars) {
                    if (car.isAvailable() && !car.isNeedsMaintenance()) {
                        filteredCars.add(car);
                    }
                }
                break;
            case "IN_USE":
                for (Car car : allCars) {
                    if (!car.isAvailable() && !car.isNeedsMaintenance()) {
                        filteredCars.add(car);
                    }
                }
                break;
            case "MAINTENANCE":
                for (Car car : allCars) {
                    if (car.isNeedsMaintenance()) {
                        filteredCars.add(car);
                    }
                }
                break;
        }
        
        carAdapter.updateCars(filteredCars);
    }

    private void updateFilterButtonStates(String activeFilter) {
        // Reset all buttons
        binding.filterAllButton.setBackgroundColor(getColor(R.color.light_grey));
        binding.filterAvailableButton.setBackgroundColor(getColor(R.color.light_grey));
        binding.filterInUseButton.setBackgroundColor(getColor(R.color.light_grey));
        binding.filterMaintenanceButton.setBackgroundColor(getColor(R.color.light_grey));
        
        // Set active button
        switch (activeFilter) {
            case "ALL":
                binding.filterAllButton.setBackgroundColor(getColor(R.color.colorPrimary));
                break;
            case "AVAILABLE":
                binding.filterAvailableButton.setBackgroundColor(getColor(R.color.available_green));
                break;
            case "IN_USE":
                binding.filterInUseButton.setBackgroundColor(getColor(R.color.in_use_orange));
                break;
            case "MAINTENANCE":
                binding.filterMaintenanceButton.setBackgroundColor(getColor(R.color.maintenance_red));
                break;
        }
    }

    @Override
    public void onCarClick(Car car) {
        Intent intent = new Intent(this, CarDetailsActivity.class);
        intent.putExtra("carId", car.getCarId());
        startActivity(intent);
    }

    @Override
    public void onMaintenanceClick(Car car) {
        if (car.isNeedsMaintenance()) {
            // Complete maintenance
            new AlertDialog.Builder(this)
                    .setTitle("Complete Maintenance")
                    .setMessage("Mark maintenance as complete for " + car.getCarId() + "?")
                    .setPositiveButton("Complete", (dialog, which) -> {
                        FleetManager.markMaintenanceComplete(car.getCarId(), new FleetManager.MaintenanceCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(MainActivity.this, "Maintenance completed", Toast.LENGTH_SHORT).show();
                                loadCars();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(MainActivity.this, "Failed to complete maintenance", Toast.LENGTH_SHORT).show();
                            }
                        });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Schedule maintenance
            showScheduleMaintenanceDialog(car);
        }
    }

    private void showScheduleMaintenanceDialog(Car car) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_schedule_maintenance, null);
        TextInputEditText notesEditText = dialogView.findViewById(R.id.notesEditText);
        
        new AlertDialog.Builder(this)
                .setTitle("Schedule Maintenance for " + car.getCarId())
                .setView(dialogView)
                .setPositiveButton("Schedule", (dialog, which) -> {
                    String notes = notesEditText.getText().toString().trim();
                    if (notes.isEmpty()) {
                        notes = "Routine maintenance";
                    }
                    
                    FleetManager.scheduleMaintenanceForCar(car.getCarId(), notes, new FleetManager.MaintenanceCallback() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(MainActivity.this, "Maintenance scheduled", Toast.LENGTH_SHORT).show();
                            loadCars();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(MainActivity.this, "Failed to schedule maintenance", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}