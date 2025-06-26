package com.cristeabogdan.fleetadmin.database;

import android.util.Log;

import com.cristeabogdan.fleetadmin.models.Car;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FleetManager {
    private static final String TAG = "FleetManager";
    private static final String CARS_COLLECTION = "cars";
    private static final String MAINTENANCE_COLLECTION = "maintenance";
    
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface FleetCallback {
        void onSuccess(List<Car> cars);
        void onFailure(Exception e);
    }

    public interface MaintenanceCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void getAllCars(FleetCallback callback) {
        db.collection(CARS_COLLECTION)
                .orderBy("carId", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Car> cars = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Car car = documentToCar(doc);
                        if (car != null) {
                            cars.add(car);
                        }
                    }
                    Log.d(TAG, "Retrieved " + cars.size() + " cars from database");
                    if (callback != null) {
                        callback.onSuccess(cars);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting cars", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void getAvailableCars(FleetCallback callback) {
        db.collection(CARS_COLLECTION)
                .whereEqualTo("isAvailable", true)
                .orderBy("carId", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Car> cars = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Car car = documentToCar(doc);
                        if (car != null) {
                            cars.add(car);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(cars);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting available cars", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void getCarsNeedingMaintenance(FleetCallback callback) {
        db.collection(CARS_COLLECTION)
                .whereEqualTo("needsMaintenance", true)
                .orderBy("carId", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Car> cars = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Car car = documentToCar(doc);
                        if (car != null) {
                            cars.add(car);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(cars);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting cars needing maintenance", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void scheduleMaintenanceForCar(String carId, String notes, MaintenanceCallback callback) {
        // Update car status
        Map<String, Object> carUpdates = new HashMap<>();
        carUpdates.put("needsMaintenance", true);
        carUpdates.put("maintenanceNotes", notes);
        carUpdates.put("isAvailable", false);
        carUpdates.put("lastUpdated", System.currentTimeMillis());

        db.collection(CARS_COLLECTION)
                .document(carId)
                .update(carUpdates)
                .addOnSuccessListener(aVoid -> {
                    // Create maintenance record
                    Map<String, Object> maintenanceData = new HashMap<>();
                    maintenanceData.put("carId", carId);
                    maintenanceData.put("notes", notes);
                    maintenanceData.put("status", "SCHEDULED");
                    maintenanceData.put("scheduledAt", System.currentTimeMillis());
                    maintenanceData.put("scheduledBy", "Fleet Admin");

                    db.collection(MAINTENANCE_COLLECTION)
                            .add(maintenanceData)
                            .addOnSuccessListener(documentReference -> {
                                Log.d(TAG, "Maintenance scheduled for car " + carId);
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error creating maintenance record", e);
                                if (callback != null) {
                                    callback.onFailure(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating car for maintenance", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void markMaintenanceComplete(String carId, MaintenanceCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("needsMaintenance", false);
        updates.put("maintenanceNotes", null);
        updates.put("isAvailable", true);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection(CARS_COLLECTION)
                .document(carId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Maintenance completed for car " + carId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error completing maintenance", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private static Car documentToCar(DocumentSnapshot doc) {
        try {
            Car car = new Car();
            car.setCarId(doc.getString("carId"));
            car.setLatitude(doc.getDouble("latitude") != null ? doc.getDouble("latitude") : 0.0);
            car.setLongitude(doc.getDouble("longitude") != null ? doc.getDouble("longitude") : 0.0);
            car.setH3Index(doc.getString("h3Index"));
            car.setAvailable(doc.getBoolean("isAvailable") != null ? doc.getBoolean("isAvailable") : false);
            car.setDriverName(doc.getString("driverName"));
            car.setCarModel(doc.getString("carModel"));
            car.setLicensePlate(doc.getString("licensePlate"));
            car.setCreatedAt(doc.getLong("createdAt"));
            car.setLastUpdated(doc.getLong("lastUpdated"));
            car.setNeedsMaintenance(doc.getBoolean("needsMaintenance") != null ? doc.getBoolean("needsMaintenance") : false);
            car.setMaintenanceNotes(doc.getString("maintenanceNotes"));
            
            return car;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to car", e);
            return null;
        }
    }
}