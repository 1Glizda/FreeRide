package com.cristeabogdan.freeride.maps;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.cristeabogdan.freeride.payment.PaymentActivity;
import com.google.android.gms.maps.model.LatLng;
import com.cristeabogdan.freeride.network.NetworkService;
import com.cristeabogdan.freeride.h3.CarManager;
import com.cristeabogdan.freeride.database.Car;
import com.cristeabogdan.freeride.database.FirestoreManager;
import com.cristeabogdan.simulator.WebSocket;
import com.cristeabogdan.simulator.WebSocketListener;
import com.cristeabogdan.freeride.utils.Constants;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MapsPresenter implements WebSocketListener {
    private static final String TAG = "MapsPresenter";

    private final NetworkService networkService;
    private MapsView view;
    private WebSocket webSocket;
    private boolean carsInitialized = false;
    private boolean isBooking = false;
    public static String RideId;
    private String assignedCarId;

    public MapsPresenter(NetworkService networkService) {
        this.networkService = networkService;
    }

    public void onAttach(MapsView view) {
        this.view = view;
        webSocket = networkService.createWebSocket(this);
        webSocket.connect();
    }

    public void onDetach() {
        if (webSocket != null) webSocket.disconnect();
        view = null;
    }

    /** Call this *after* you have a valid LatLng (e.g. in your location‐callback). */
    public void seedCarsIfNeeded(double lat, double lng) {
        if (carsInitialized) return;
        CarManager.initializeCarsIfNeeded(lat, lng, new CarManager.InitCB() {
            @Override
            public void onSuccess(List<Car> cars) {
                Log.d(TAG, "Cars initialized: " + cars.size());
                carsInitialized = true;
                loadAvailableCars();
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Car init failed", e);
                carsInitialized = true;
                loadAvailableCars();
            }
        });
    }

    public void requestNearbyCabs(double lat, double lng) {
        if (!carsInitialized) {
            if (view != null) view.showDirectionApiFailedError("Please wait, still loading cars…");
            return;
        }
        loadAvailableCars();
    }

    private void loadAvailableCars() {
        FirestoreManager.get().getAllCars(new FirestoreManager.QueryCallback<List<Car>>() {
            @Override
            public void onSuccess(List<Car> cars) {
                if (view == null) return;
                List<LatLng> locs = new ArrayList<>();
                for (Car c : cars) {
                    if (c.isAvailable()) {
                        locs.add(new LatLng(c.getLatitude(), c.getLongitude()));
                    }
                }
                view.showNearbyCabs(locs);
            }
            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "loadAvailableCars failed", e);
            }
        });
    }

    private void saveRideToFirestore(String rideId, String userId,
                                     String vehicleId) {
        Map<String,Object> ride = new HashMap<>();
        ride.put("rideId", rideId);
        ride.put("userId", userId);
        ride.put("vehicleId",vehicleId);
        ride.put("duration", PaymentActivity.MINUTES);
        ride.put("distance", PaymentActivity.KM);
        ride.put("timestamp", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("rides")
                .document(rideId)
                .set(ride);
    }

    public void requestCab(LatLng pickUp, LatLng dropOff) {
//        if (!carsInitialized) {
//            if (view != null) view.showDirectionApiFailedError("Wait for cars to finish loading.");
//            return;
//        }
        isBooking = true;
        CarManager.findNearestAvailableCar(pickUp.latitude, pickUp.longitude, new CarManager.MatchCB() {
            @Override
            public void onCarMatched(Car car) {
                isBooking = false;
                assignedCarId = car.getCarId();
                if (view != null) view.informCabBooked();
                // **SAVE RIDE**
                String rideId = UUID.randomUUID().toString();
                RideId = rideId;
                String userId = FirebaseAuth.getInstance().getUid();
                saveRideToFirestore(rideId, userId, assignedCarId);

                startSimulation(car, pickUp, dropOff);
            }
            @Override
            public void onNoCarAvailable() {
                isBooking = false;
                if (view != null) view.showDirectionApiFailedError("No cars available. Try again later.");
            }
            @Override
            public void onFailure(Exception e) {
                isBooking = false;
                if (view != null) view.showDirectionApiFailedError("Error finding car: " + e.getMessage());
            }
        });
    }

    private void startSimulation(Car car, LatLng pickUp, LatLng dropOff) {
        if (webSocket == null) return;
        try {
            JSONObject o = new JSONObject();
            o.put("type", "requestCab");
            o.put("pickUpLat", pickUp.latitude);
            o.put("pickUpLng", pickUp.longitude);
            o.put("dropLat", dropOff.latitude);
            o.put("dropLng", dropOff.longitude);
            o.put("carLat", car.getLatitude());
            o.put("carLng", car.getLongitude());
            o.put("assignedCarId", assignedCarId);
            webSocket.sendMessage(o.toString());
        } catch (JSONException e) {
            Log.e(TAG, "startSimulation JSON error", e);
        }
    }

    // --- WebSocketListener callbacks ---

    @Override
    public void onConnect() {
        Log.d(TAG, "WebSocket connected");
    }

    @Override
    public void onMessage(String data) {
        try {
            JSONObject obj = new JSONObject(data);
            String type = obj.getString("type");
            switch (type) {
                case Constants.NEAR_BY_CABS:
                    if (isBooking) return;
                    JSONArray arr = obj.getJSONArray(Constants.LOCATIONS);
                    List<LatLng> locs = new ArrayList<>();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject loc = arr.getJSONObject(i);
                        locs.add(new LatLng(loc.getDouble(Constants.LAT), loc.getDouble(Constants.LNG)));
                    }
                    if (view != null) view.showNearbyCabs(locs);
                    break;

                case Constants.CAB_BOOKED:
                    if (view != null) view.informCabBooked();
                    break;

                case Constants.PICKUP_PATH:
                case Constants.TRIP_PATH:
                    handlePath(obj);
                    break;

                case Constants.LOCATION:
                    handleLocation(obj);
                    break;

                case Constants.CAB_IS_ARRIVING:
                    if (view != null) view.informCabIsArriving();
                    break;

                case Constants.CAB_ARRIVED:
                    if (view != null) view.informCabArrived();
                    break;

                case Constants.TRIP_START:
                    if (view != null) view.informTripStart();
                    break;

                case Constants.TRIP_END:
                    if (view != null) view.informTripEnd();
                    double finalLat = obj.optDouble("finalLat", 0);
                    double finalLng = obj.optDouble("finalLng", 0);
                    if (assignedCarId != null && finalLat != 0 && finalLng != 0) {
                        CarManager.releaseCarAfterTrip(assignedCarId, finalLat, finalLng);
                        assignedCarId = null;
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown message type: " + type);
            }
        } catch (JSONException e) {
            Log.e(TAG, "onMessage parse error", e);
        }
    }

    private void handlePath(JSONObject obj) throws JSONException {
        JSONArray arr = obj.getJSONArray("path");
        List<LatLng> path = new ArrayList<>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject p = arr.getJSONObject(i);
            path.add(new LatLng(p.getDouble("lat"), p.getDouble("lng")));
        }
        if (view != null) view.showPath(path);
    }

    private void handleLocation(JSONObject obj) throws JSONException {
        LatLng loc = new LatLng(obj.getDouble("lat"), obj.getDouble("lng"));
        if (view != null) view.updateCabLocation(loc);
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "WebSocket disconnected");
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "WebSocket error: " + error);
        try {
            JSONObject obj = new JSONObject(error);
            switch (obj.getString("type")) {
                case Constants.ROUTES_NOT_AVAILABLE:
                    if (view != null) view.showRoutesNotAvailableError();
                    break;
                case Constants.DIRECTION_API_FAILED:
                    if (view != null)
                        view.showDirectionApiFailedError("Direction API failed: " + obj.getString("error"));
                    break;
                default:
                    Log.w(TAG, "Unhandled error type");
            }
        } catch (JSONException e) {
            Log.e(TAG, "onError parse failed", e);
        }
    }
}
