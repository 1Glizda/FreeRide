package com.cristeabogdan.freeride.h3;

import android.util.Log;
import com.cristeabogdan.freeride.database.Car;
import com.cristeabogdan.freeride.database.FirebaseCarManager;

import java.util.List;

public class CarManager {
    private static final String TAG = "CarManager";

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
        FirebaseCarManager.initializeCarsIfNeeded(centerLat, centerLng, new FirebaseCarManager.CarInitializationCallback() {
            @Override
            public void onSuccess(List<Car> cars) {
                Log.d(TAG, "Cars initialized successfully: " + cars.size());
                if (callback != null) {
                    callback.onSuccess(cars);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to initialize cars", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public static void findNearestAvailableCar(double pickupLat, double pickupLng, CarMatchingCallback callback) {
        FirebaseCarManager.findNearestAvailableCar(pickupLat, pickupLng, new FirebaseCarManager.CarMatchingCallback() {
            @Override
            public void onCarMatched(Car car) {
                Log.d(TAG, "Car matched: " + car.getCarId() + " - " + car.getDriverName());
                if (callback != null) {
                    callback.onCarMatched(car);
                }
            }

            @Override
            public void onNoCarAvailable() {
                Log.w(TAG, "No cars available in the area");
                if (callback != null) {
                    callback.onNoCarAvailable();
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error finding car", e);
                if (callback != null) {
                    callback.onFailure(e);
                }
            }
        });
    }

    public static void releaseCarAfterTrip(String carId, double finalLat, double finalLng) {
        FirebaseCarManager.releaseCarAfterTrip(carId, finalLat, finalLng);
    }

    public static void getAllAvailableCars(FirebaseCarManager.CarQueryCallback callback) {
        FirebaseCarManager.getAllAvailableCars(callback);
    }
}