package com.cristeabogdan.freeride.database;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class FirestoreManager {
    private static final String TAG = "FirestoreManager";
    private static FirestoreManager instance;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private FirestoreManager() {}

    public static synchronized void init() {
        if (instance == null) instance = new FirestoreManager();
    }

    public static FirestoreManager get() {
        if (instance == null) throw new IllegalStateException("Call init() first");
        return instance;
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface QueryCallback<T> {
        void onSuccess(T result);
        void onFailure(Exception e);
    }

    public void insertCar(Car car, UpdateCallback cb) {
        db.collection("cars")
                .document(car.getCarId())
                .set(car)
                .addOnSuccessListener(a -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    public void updateCarLocation(String id, double lat, double lng, String h3, UpdateCallback cb) {
        db.collection(getCars()).document(id)
                .update(
                        "latitude", lat,
                        "longitude", lng,
                        "h3Index", h3,
                        "location", new com.google.firebase.firestore.GeoPoint(lat, lng)
                )
                .addOnSuccessListener(a -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    @NonNull
    private static String getCars() {
        return "cars";
    }

    public void updateCarAvailability(String id, boolean avail, UpdateCallback cb) {
        db.collection("cars").document(id)
                .update("available", avail)
                .addOnSuccessListener(a -> cb.onSuccess())
                .addOnFailureListener(cb::onFailure);
    }

    public void getAllCars(QueryCallback<List<Car>> cb) {
        db.collection("cars")
                .get()
                .addOnSuccessListener(snaps -> {
                    List<Car> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc: snaps) {
                        list.add(doc.toObject(Car.class));
                    }
                    cb.onSuccess(list);
                })
                .addOnFailureListener(cb::onFailure);
    }

    // inside FirestoreManager
    public void getCarsInH3Indices(List<String> h3s, QueryCallback<List<Car>> cb) {
        Log.d(TAG, "Querying H3 indices: " + h3s);
        final int BATCH_SIZE = 10;
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < h3s.size(); i += BATCH_SIZE) {
            batches.add(h3s.subList(i, Math.min(i + BATCH_SIZE, h3s.size())));
        }

        List<Car> allCars = new ArrayList<>();
        AtomicInteger completedBatches = new AtomicInteger(0);
        final int totalBatches = batches.size();

        for (List<String> batch : batches) {
            db.collection("cars")
                    .whereIn("h3Index", batch)
                    .whereEqualTo("available", true)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        synchronized (allCars) {
                            for (QueryDocumentSnapshot doc : snapshot) {
                                allCars.add(doc.toObject(Car.class));
                            }
                        }
                        if (completedBatches.incrementAndGet() == totalBatches) {
                            cb.onSuccess(allCars);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (completedBatches.incrementAndGet() == totalBatches) {
                            // Return partial results on batch failure
                            cb.onSuccess(allCars);
                        }
                    });
        }
    }
}
