package com.cristeabogdan.freeride.h3;

import android.util.Log;

import com.cristeabogdan.freeride.database.Car;
import com.cristeabogdan.freeride.database.FirestoreManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class CarManager {
    private static final String TAG = "CarManager";
    private static final double SQR_KM = 2.0;
    private static final String[] MODELS = {
            "Tesla Model 3","Waymo Jaguar","Cruise Origin","Tesla Model Y",
            "Tesla Model X","Tesla Model S","Tesla Model 3","Waymo Jaguar",
            "Tesla Model Y","Tesla Model 3"
    };

    public interface InitCB {
        void onSuccess(List<Car> cars);
        void onFailure(Exception e);
    }
    public interface MatchCB {
        void onCarMatched(Car car);
        void onNoCarAvailable();
        void onFailure(Exception e);
    }

    public static void initializeCarsIfNeeded(double lat, double lng, InitCB cb) {
//        FirestoreManager.get().getAllCars(new FirestoreManager.QueryCallback<List<Car>>() {
//            @Override public void onSuccess(List<Car> existing) {
//                if (existing.isEmpty()) generateRandomCars(lat, lng, cb);
//                else cb.onSuccess(existing);
//            }
//            @Override public void onFailure(Exception e) {
//                Log.e(TAG,"init fail",e);
//                generateRandomCars(lat, lng, cb);
//            }
//        });
        generateRandomCars(lat, lng, cb);
    }

    public static boolean getRandomBoolean() {
        return Math.random() < 0.5;
        //I tried another approaches here, still the same result
    }

    private static void generateRandomCars(double clat, double clng, InitCB cb) {
        List<Car> cars = new ArrayList<>();
        Random r = new Random();
        double latR = SQR_KM/111.0;
        double lngR = SQR_KM/(111.0*Math.cos(Math.toRadians(clat)));

        for (int i = 0; i < MODELS.length; i++) {
            double la = clat + (r.nextDouble() - 0.5) * latR;
            double lo = clng + (r.nextDouble() - 0.5) * lngR;
            String id = String.format("CAR_%03d", i + 1);
            String h3 = String.valueOf(H3Manager.getH3Index(la, lo, H3Manager.H3_RESOLUTION));
            String plate = genPlate(r);
            Car c = new Car(id, la, lo, h3, true, MODELS[i], plate); // All cars start as available
            cars.add(c);
        }

        AtomicInteger insertCount = new AtomicInteger(0);
        for (Car car : cars) {
            FirestoreManager.get().insertCar(car, new FirestoreManager.UpdateCallback() {
                @Override
                public void onSuccess() {
                    if (insertCount.incrementAndGet() == cars.size()) {
                        cb.onSuccess(cars);
                    }
                }
                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "insert", e);
                    if (insertCount.incrementAndGet() == cars.size()) {
                        cb.onSuccess(cars);
                    }
                }
            });
        }
    }

    private static String genPlate(Random random) {
        StringBuilder plate = new StringBuilder("B");
        for (int i = 0; i < 3; i++) plate.append(random.nextInt(10));
        for (int i = 0; i < 3; i++) plate.append((char) ('A' + random.nextInt(26)));
        return plate.toString();
    }

    public static void findNearestAvailableCar(double lat, double lng, MatchCB cb) {
        findInRings(lat, lng, 0, 5, cb);
    }

    private static void findInRings(double lat, double lng, int ring, int max, MatchCB cb) {
        if (ring > max) {
            cb.onNoCarAvailable();
            return;
        }

        Log.d(TAG, "Searching ring " + ring + " of " + max);
        List<Long> neighborIndices = H3Manager.getNeighborH3Indices(lat, lng, ring);
        Log.d(TAG, "H3 indices being queried: " + neighborIndices);

        List<String> h3s = new ArrayList<>();
        for (Long idx : neighborIndices) {
            h3s.add(String.valueOf(idx));
        }
        Log.d(TAG, "Querying " + h3s.size() + " indices at ring " + ring);

        FirestoreManager.get().getCarsInH3Indices(h3s, new FirestoreManager.QueryCallback<List<Car>>() {
            @Override
            public void onSuccess(List<Car> cars) {
                Log.d(TAG, "request cab success");
                List<Car> availableCars = new ArrayList<>();
                Log.d(TAG, "created availablecars list, cars in given list" + cars);
                for (Car car : cars) {
                    Log.d(TAG, "the cars parameter is valid and it traverses");
                    if (car.isAvailable()) {
                        Log.d(TAG, "selected car is available");
                        availableCars.add(car);
                    }
                }

                if (!availableCars.isEmpty()) {
                    Car nearest = null;
                    double minDistance = Double.MAX_VALUE;

                    for (Car car : availableCars) {
                        double distance = H3Manager.getDistanceKm(lat, lng, car.getLatitude(), car.getLongitude());
                        if (distance < minDistance) {
                            minDistance = distance;
                            nearest = car;
                        }
                    }

                    if (nearest != null) {
                        Car finalNearest = nearest;
                        FirestoreManager.get().updateCarAvailability(nearest.getCarId(), false,
                                new FirestoreManager.UpdateCallback() {
                                    @Override
                                    public void onSuccess() {
                                        cb.onCarMatched(finalNearest);
                                    }
                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Availability update failed", e);
                                        cb.onCarMatched(finalNearest); // Still proceed with booking
                                    }
                                });
                        return;
                    }
                }
                // Only search next ring if no available cars found in current ring
                if (availableCars.isEmpty()) {
                    findInRings(lat, lng, ring + 1, max, cb);
                }
                Log.w(TAG, "no cars found, but no failure");
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Ring search failed", e);
                // Proceed to next ring even on failure
                findInRings(lat, lng, ring + 1, max, cb);
            }
        });
    }

    public static void releaseCarAfterTrip(String id, double la, double lo) {
        String h3 = String.valueOf(H3Manager.getH3Index(la, lo, H3Manager.H3_RESOLUTION));
        FirestoreManager.get().updateCarLocation(id, la, lo, h3, new FirestoreManager.UpdateCallback() {
            @Override public void onSuccess() {
                FirestoreManager.get().updateCarAvailability(id, true, new FirestoreManager.UpdateCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onFailure(Exception e) { Log.e(TAG,"release",e); }
                });
            }
            @Override public void onFailure(Exception e) { Log.e(TAG,"loc",e); }
        });
    }
}
