package com.cristeabogdan.freeride.database;

import android.util.Log;
import com.cristeabogdan.freeride.h3.H3Manager;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class FirebaseCarManager {
    private static final String TAG = "FirebaseCarManager";
    private static final String CARS_COLLECTION = "cars";
    private static final double SQUARE_SIZE_KM = 5.0;
    private static final int NUM_CARS = 10;
    
    private static final String[] DRIVER_NAMES = {
        "John Smith", "Maria Garcia", "David Johnson", "Sarah Wilson", "Michael Brown",
        "Lisa Davis", "Robert Miller", "Jennifer Taylor", "William Anderson", "Jessica Thomas"
    };
    
    private static final String[] CAR_MODELS = {
        "Toyota Camry", "Honda Civic", "Ford Focus", "Nissan Altima", "Chevrolet Malibu",
        "Hyundai Elantra", "Volkswagen Jetta", "Mazda3", "Subaru Impreza", "Kia Forte"
    };

    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface CarInitializationCallback {
        void onSuccess(List<Car> cars);
        void onFailure(Exception e);
    }

    public interface CarMatchingCallback {
        void onCarMatched(Car car);
        void onNoCarAvailable();
        void onFailure(Exception e);
    }

    public interface CarQueryCallback {
        void onSuccess(List<Car> cars);
        void onFailure(Exception e);
    }

    public interface CarUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void initializeCarsIfNeeded(double centerLat, double centerLng, CarInitializationCallback callback) {
        db.collection(CARS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        Log.d(TAG, "No cars found, generating new ones");
                        generateRandomCars(centerLat, centerLng, callback);
                    } else {
                        Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " existing cars");
                        List<Car> existingCars = new ArrayList<>();
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Car car = documentToCar(doc);
                            if (car != null) {
                                existingCars.add(car);
                            }
                        }
                        if (callback != null) {
                            callback.onSuccess(existingCars);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking existing cars, generating new ones", e);
                    generateRandomCars(centerLat, centerLng, callback);
                });
    }

    private static void generateRandomCars(double centerLat, double centerLng, CarInitializationCallback callback) {
        List<Car> cars = new ArrayList<>();
        Random random = new Random();
        
        double latRange = SQUARE_SIZE_KM / 111.0;
        double lngRange = SQUARE_SIZE_KM / (111.0 * Math.cos(Math.toRadians(centerLat)));

        for (int i = 0; i < NUM_CARS; i++) {
            double randomLat = centerLat + (random.nextDouble() - 0.5) * latRange;
            double randomLng = centerLng + (random.nextDouble() - 0.5) * lngRange;
            
            String carId = "CAR_" + String.format("%03d", i + 1);
            String h3Index = H3Manager.getH3Index(randomLat, randomLng);
            
            Car car = new Car(
                carId,
                randomLat,
                randomLng,
                h3Index,
                true,
                DRIVER_NAMES[i],
                CAR_MODELS[i],
                generateLicensePlate(random)
            );
            
            cars.add(car);
            
            // Save to Firestore
            Map<String, Object> carData = new HashMap<>();
            carData.put("carId", car.getCarId());
            carData.put("latitude", car.getLatitude());
            carData.put("longitude", car.getLongitude());
            carData.put("location", new GeoPoint(car.getLatitude(), car.getLongitude()));
            carData.put("h3Index", car.getH3Index());
            carData.put("isAvailable", car.isAvailable());
            carData.put("driverName", car.getDriverName());
            carData.put("carModel", car.getCarModel());
            carData.put("licensePlate", car.getLicensePlate());
            carData.put("createdAt", System.currentTimeMillis());

            db.collection(CARS_COLLECTION)
                    .document(carId)
                    .set(carData)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Car " + carId + " saved successfully"))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to save car " + carId, e));
        }

        Log.d(TAG, "Generated " + cars.size() + " random cars");
        if (callback != null) {
            callback.onSuccess(cars);
        }
    }

    private static String generateLicensePlate(Random random) {
        StringBuilder plate = new StringBuilder();
        for (int i = 0; i < 3; i++) {
            plate.append((char) ('A' + random.nextInt(26)));
        }
        plate.append("-");
        for (int i = 0; i < 4; i++) {
            plate.append(random.nextInt(10));
        }
        return plate.toString();
    }

    public static void findNearestAvailableCar(double pickupLat, double pickupLng, CarMatchingCallback callback) {
        findCarInExpandingRings(pickupLat, pickupLng, 0, 3, callback);
    }

    private static void findCarInExpandingRings(double pickupLat, double pickupLng, int currentRing, int maxRings, CarMatchingCallback callback) {
        if (currentRing > maxRings) {
            if (callback != null) {
                callback.onNoCarAvailable();
            }
            return;
        }

        List<String> h3Indices = H3Manager.getNeighborH3Indices(pickupLat, pickupLng, currentRing);
        
        db.collection(CARS_COLLECTION)
                .whereIn("h3Index", h3Indices)
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        Car closestCar = null;
                        double minDistance = Double.MAX_VALUE;
                        
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Car car = documentToCar(doc);
                            if (car != null) {
                                double distance = H3Manager.getDistanceKm(
                                    pickupLat, pickupLng, 
                                    car.getLatitude(), car.getLongitude()
                                );
                                
                                if (distance < minDistance) {
                                    minDistance = distance;
                                    closestCar = car;
                                }
                            }
                        }
                        
                        if (closestCar != null) {
                            // Mark car as unavailable
                            updateCarAvailability(closestCar.getCarId(), false, new CarUpdateCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Car " + closestCar.getCarId() + " assigned and marked unavailable");
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Failed to update car availability", e);
                                }
                            });
                            
                            if (callback != null) {
                                callback.onCarMatched(closestCar);
                            }
                        } else {
                            findCarInExpandingRings(pickupLat, pickupLng, currentRing + 1, maxRings, callback);
                        }
                    } else {
                        findCarInExpandingRings(pickupLat, pickupLng, currentRing + 1, maxRings, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying cars in ring " + currentRing, e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void updateCarLocation(String carId, double latitude, double longitude, String h3Index, CarUpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);
        updates.put("location", new GeoPoint(latitude, longitude));
        updates.put("h3Index", h3Index);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection(CARS_COLLECTION)
                .document(carId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void updateCarAvailability(String carId, boolean isAvailable, CarUpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isAvailable", isAvailable);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection(CARS_COLLECTION)
                .document(carId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void getAllAvailableCars(CarQueryCallback callback) {
        db.collection(CARS_COLLECTION)
                .whereEqualTo("isAvailable", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Car> availableCars = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Car car = documentToCar(doc);
                        if (car != null) {
                            availableCars.add(car);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(availableCars);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void releaseCarAfterTrip(String carId, double finalLat, double finalLng) {
        String newH3Index = H3Manager.getH3Index(finalLat, finalLng);
        
        updateCarLocation(carId, finalLat, finalLng, newH3Index, new CarUpdateCallback() {
            @Override
            public void onSuccess() {
                updateCarAvailability(carId, true, new CarUpdateCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Car " + carId + " released and available again");
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "Failed to make car available", e);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to update car location", e);
            }
        });
    }

    private static Car documentToCar(DocumentSnapshot doc) {
        try {
            return new Car(
                doc.getString("carId"),
                doc.getDouble("latitude"),
                doc.getDouble("longitude"),
                doc.getString("h3Index"),
                doc.getBoolean("isAvailable"),
                doc.getString("driverName"),
                doc.getString("carModel"),
                doc.getString("licensePlate")
            );
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to car", e);
            return null;
        }
    }
}