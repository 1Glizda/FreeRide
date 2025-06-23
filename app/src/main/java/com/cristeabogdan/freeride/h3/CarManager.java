package com.cristeabogdan.freeride.h3;

import android.util.Log;
import com.cristeabogdan.freeride.database.Car;
import com.cristeabogdan.freeride.database.MongoDBManager;
import com.google.android.gms.maps.model.LatLng;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CarManager {
    private static final String TAG = "CarManager";
    private static final double SQUARE_SIZE_KM = 5.0; // 5km square
    private static final int NUM_CARS = 10;
    
    private static final String[] DRIVER_NAMES = {
        "John Smith", "Maria Garcia", "David Johnson", "Sarah Wilson", "Michael Brown",
        "Lisa Davis", "Robert Miller", "Jennifer Taylor", "William Anderson", "Jessica Thomas"
    };
    
    private static final String[] CAR_MODELS = {
        "Toyota Camry", "Honda Civic", "Ford Focus", "Nissan Altima", "Chevrolet Malibu",
        "Hyundai Elantra", "Volkswagen Jetta", "Mazda3", "Subaru Impreza", "Kia Forte"
    };

    public interface CarInitializationCallback {
        void onSuccess(List<Car> cars);
        void onFailure(Exception e);
    }

    public interface CarMatchingCallback {
        void onCarMatched(Car car);
        void onNoCarAvailable();
        void onFailure(Exception e);
    }

    public static void initializeCarsIfNeeded(double centerLat, double centerLng, CarInitializationCallback callback) {
        MongoDBManager.getInstance().getAllCars(new MongoDBManager.CarQueryCallback() {
            @Override
            public void onSuccess(List<Car> existingCars) {
                if (existingCars.isEmpty()) {
                    Log.d(TAG, "No cars found, generating new ones");
                    generateRandomCars(centerLat, centerLng, callback);
                } else {
                    Log.d(TAG, "Found " + existingCars.size() + " existing cars");
                    if (callback != null) {
                        callback.onSuccess(existingCars);
                    }
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error checking existing cars, generating new ones", e);
                generateRandomCars(centerLat, centerLng, callback);
            }
        });
    }

    private static void generateRandomCars(double centerLat, double centerLng, CarInitializationCallback callback) {
        List<Car> cars = new ArrayList<>();
        Random random = new Random();
        
        // Convert 5km to approximate degrees (rough approximation)
        double latRange = SQUARE_SIZE_KM / 111.0; // 1 degree lat ≈ 111km
        double lngRange = SQUARE_SIZE_KM / (111.0 * Math.cos(Math.toRadians(centerLat)));

        for (int i = 0; i < NUM_CARS; i++) {
            // Generate random position within 5km square
            double randomLat = centerLat + (random.nextDouble() - 0.5) * latRange;
            double randomLng = centerLng + (random.nextDouble() - 0.5) * lngRange;
            
            String carId = "CAR_" + String.format("%03d", i + 1);
            String h3Index = H3Manager.getH3Index(randomLat, randomLng);
            
            Car car = new Car(
                carId,
                randomLat,
                randomLng,
                h3Index,
                true, // Available
                DRIVER_NAMES[i],
                CAR_MODELS[i],
                generateLicensePlate(random)
            );
            
            cars.add(car);
            
            // Insert into MongoDB
            MongoDBManager.getInstance().insertCar(car, new MongoDBManager.CarUpdateCallback() {
                @Override
                public void onSuccess() {
                    Log.d(TAG, "Car " + car.getCarId() + " inserted successfully");
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to insert car " + car.getCarId(), e);
                }
            });
        }

        Log.d(TAG, "Generated " + cars.size() + " random cars");
        if (callback != null) {
            callback.onSuccess(cars);
        }
    }

    private static String generateLicensePlate(Random random) {
        StringBuilder plate = new StringBuilder();
        // Generate format: ABC-1234
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
        // Get H3 indices in expanding rings until we find a car
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
        
        MongoDBManager.getInstance().getCarsInH3Indices(h3Indices, new MongoDBManager.CarQueryCallback() {
            @Override
            public void onSuccess(List<Car> cars) {
                if (!cars.isEmpty()) {
                    // Find the closest car
                    Car closestCar = null;
                    double minDistance = Double.MAX_VALUE;
                    
                    for (Car car : cars) {
                        double distance = H3Manager.getDistanceKm(
                            pickupLat, pickupLng, 
                            car.getLatitude(), car.getLongitude()
                        );
                        
                        if (distance < minDistance) {
                            minDistance = distance;
                            closestCar = car;
                        }
                    }
                    
                    if (closestCar != null) {
                        // Mark car as unavailable
                        MongoDBManager.getInstance().updateCarAvailability(
                            closestCar.getCarId(), 
                            false, 
                            new MongoDBManager.CarUpdateCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, "Car " + closestCar.getCarId() + " assigned and marked unavailable");
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "Failed to update car availability", e);
                                }
                            }
                        );
                        
                        if (callback != null) {
                            callback.onCarMatched(closestCar);
                        }
                    } else {
                        // Try next ring
                        findCarInExpandingRings(pickupLat, pickupLng, currentRing + 1, maxRings, callback);
                    }
                } else {
                    // Try next ring
                    findCarInExpandingRings(pickupLat, pickupLng, currentRing + 1, maxRings, callback);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error querying cars in ring " + currentRing, e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public static void releaseCarAfterTrip(String carId, double finalLat, double finalLng) {
        String newH3Index = H3Manager.getH3Index(finalLat, finalLng);
        
        // Update car location and make it available again
        MongoDBManager.getInstance().updateCarLocation(carId, finalLat, finalLng, newH3Index, 
            new MongoDBManager.CarUpdateCallback() {
                @Override
                public void onSuccess() {
                    MongoDBManager.getInstance().updateCarAvailability(carId, true, 
                        new MongoDBManager.CarUpdateCallback() {
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

    public static void getAllAvailableCars(MongoDBManager.CarQueryCallback callback) {
        MongoDBManager.getInstance().getAllCars(new MongoDBManager.CarQueryCallback() {
            @Override
            public void onSuccess(List<Car> allCars) {
                List<Car> availableCars = new ArrayList<>();
                for (Car car : allCars) {
                    if (car.isAvailable()) {
                        availableCars.add(car);
                    }
                }
                if (callback != null) {
                    callback.onSuccess(availableCars);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }
}