package com.cristeabogdan.freeride.database;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RideManager {
    private static final String TAG = "RideManager";
    private static final String RIDES_COLLECTION = "rides";
    private static final String FEEDBACK_COLLECTION = "feedback";
    
    private static FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface RideCallback {
        void onSuccess(String rideId);
        void onFailure(Exception e);
    }

    public interface RideQueryCallback {
        void onSuccess(List<Ride> rides);
        void onFailure(Exception e);
    }

    public interface RideUpdateCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    public static void createRide(String userId, String carId, double pickupLat, double pickupLng, 
                                 double dropLat, double dropLng, RideCallback callback) {
        Map<String, Object> rideData = new HashMap<>();
        rideData.put("userId", userId);
        rideData.put("carId", carId);
        rideData.put("pickupLocation", new GeoPoint(pickupLat, pickupLng));
        rideData.put("dropLocation", new GeoPoint(dropLat, dropLng));
        rideData.put("status", "REQUESTED");
        rideData.put("requestedAt", System.currentTimeMillis());
        rideData.put("createdAt", System.currentTimeMillis());

        db.collection(RIDES_COLLECTION)
                .add(rideData)
                .addOnSuccessListener(documentReference -> {
                    String rideId = documentReference.getId();
                    Log.d(TAG, "Ride created with ID: " + rideId);
                    if (callback != null) {
                        callback.onSuccess(rideId);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating ride", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void updateRideStatus(String rideId, String status, RideUpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", status);
        
        switch (status) {
            case "DRIVER_ASSIGNED":
                updates.put("driverAssignedAt", System.currentTimeMillis());
                break;
            case "DRIVER_ARRIVING":
                updates.put("driverArrivingAt", System.currentTimeMillis());
                break;
            case "DRIVER_ARRIVED":
                updates.put("driverArrivedAt", System.currentTimeMillis());
                break;
            case "TRIP_STARTED":
                updates.put("tripStartedAt", System.currentTimeMillis());
                break;
            case "TRIP_COMPLETED":
                updates.put("tripCompletedAt", System.currentTimeMillis());
                break;
        }

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ride " + rideId + " status updated to " + status);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating ride status", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void completeRide(String rideId, double amount, String distance, String duration, RideUpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "TRIP_COMPLETED");
        updates.put("tripCompletedAt", System.currentTimeMillis());
        updates.put("amount", amount);
        updates.put("distance", distance);
        updates.put("duration", duration);

        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ride " + rideId + " completed");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error completing ride", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void getUserRides(String userId, RideQueryCallback callback) {
        db.collection(RIDES_COLLECTION)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ride> rides = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Ride ride = documentToRide(doc);
                        if (ride != null) {
                            rides.add(ride);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(rides);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting user rides", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void getCarRides(String carId, RideQueryCallback callback) {
        db.collection(RIDES_COLLECTION)
                .whereEqualTo("carId", carId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Ride> rides = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Ride ride = documentToRide(doc);
                        if (ride != null) {
                            rides.add(ride);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(rides);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting car rides", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    public static void saveFeedback(String rideId, int rating, String comment, RideUpdateCallback callback) {
        // First get the ride details
        db.collection(RIDES_COLLECTION)
                .document(rideId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> feedbackData = new HashMap<>();
                        feedbackData.put("rideId", rideId);
                        feedbackData.put("userId", documentSnapshot.getString("userId"));
                        feedbackData.put("carId", documentSnapshot.getString("carId"));
                        feedbackData.put("rating", rating);
                        feedbackData.put("comment", comment);
                        feedbackData.put("amount", documentSnapshot.getDouble("amount"));
                        feedbackData.put("distance", documentSnapshot.getString("distance"));
                        feedbackData.put("duration", documentSnapshot.getString("duration"));
                        feedbackData.put("timestamp", System.currentTimeMillis());

                        db.collection(FEEDBACK_COLLECTION)
                                .add(feedbackData)
                                .addOnSuccessListener(documentReference -> {
                                    Log.d(TAG, "Feedback saved with ID: " + documentReference.getId());
                                    
                                    // Update ride with feedback reference
                                    Map<String, Object> rideUpdate = new HashMap<>();
                                    rideUpdate.put("feedbackId", documentReference.getId());
                                    rideUpdate.put("rating", rating);
                                    
                                    db.collection(RIDES_COLLECTION)
                                            .document(rideId)
                                            .update(rideUpdate);
                                    
                                    if (callback != null) {
                                        callback.onSuccess();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error saving feedback", e);
                                    if (callback != null) {
                                        callback.onFailure(e);
                                    }
                                });
                    } else {
                        Log.e(TAG, "Ride not found for feedback");
                        if (callback != null) {
                            callback.onFailure(new Exception("Ride not found"));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting ride for feedback", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    private static Ride documentToRide(DocumentSnapshot doc) {
        try {
            Ride ride = new Ride();
            ride.setId(doc.getId());
            ride.setUserId(doc.getString("userId"));
            ride.setCarId(doc.getString("carId"));
            ride.setStatus(doc.getString("status"));
            
            GeoPoint pickupLocation = doc.getGeoPoint("pickupLocation");
            if (pickupLocation != null) {
                ride.setPickupLatitude(pickupLocation.getLatitude());
                ride.setPickupLongitude(pickupLocation.getLongitude());
            }
            
            GeoPoint dropLocation = doc.getGeoPoint("dropLocation");
            if (dropLocation != null) {
                ride.setDropLatitude(dropLocation.getLatitude());
                ride.setDropLongitude(dropLocation.getLongitude());
            }
            
            ride.setAmount(doc.getDouble("amount"));
            ride.setDistance(doc.getString("distance"));
            ride.setDuration(doc.getString("duration"));
            ride.setRating(doc.getLong("rating"));
            ride.setCreatedAt(doc.getLong("createdAt"));
            ride.setRequestedAt(doc.getLong("requestedAt"));
            ride.setTripStartedAt(doc.getLong("tripStartedAt"));
            ride.setTripCompletedAt(doc.getLong("tripCompletedAt"));
            ride.setFeedbackId(doc.getString("feedbackId"));
            
            return ride;
        } catch (Exception e) {
            Log.e(TAG, "Error converting document to ride", e);
            return null;
        }
    }
}