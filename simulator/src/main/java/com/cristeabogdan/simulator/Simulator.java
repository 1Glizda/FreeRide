package com.cristeabogdan.simulator;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class Simulator {

    private static final String TAG = "Simulator";
    private static Timer timer;
    private static TimerTask timerTask;
    public static GeoApiContext geoApiContext;
    private static LatLng currentLocation;
    private static LatLng pickUpLocation;
    private static LatLng dropLocation;
    private static ArrayList<LatLng> nearbyCabLocations = new ArrayList<>();
    private static ArrayList<LatLng> pickUpPath = new ArrayList<>();
    private static ArrayList<LatLng> tripPath = new ArrayList<>();
    private static final Handler mainThread = new Handler(Looper.getMainLooper());
    private static final Random random = new Random();

    public static void getFakeNearbyCabLocations(
            double latitude,
            double longitude,
            WebSocketListener webSocketListener
    ) {
        nearbyCabLocations.clear();
        currentLocation = new LatLng(latitude, longitude);
        int size = random.nextInt(3) + 4; // (4..6).random()

        for (int i = 1; i <= size; i++) {
            int randomOperatorForLat = random.nextInt(2);
            int randomOperatorForLng = random.nextInt(2);
            double randomDeltaForLat = (random.nextInt(41) + 10) / 10000.00; // (10..50).random()
            double randomDeltaForLng = (random.nextInt(41) + 10) / 10000.00;

            if (randomOperatorForLat == 1) {
                randomDeltaForLat *= -1;
            }
            if (randomOperatorForLng == 1) {
                randomDeltaForLng *= -1;
            }

            double randomLatitude = Math.min(latitude + randomDeltaForLat, 90.00);
            double randomLongitude = Math.min(longitude + randomDeltaForLng, 180.00);
            nearbyCabLocations.add(new LatLng(randomLatitude, randomLongitude));
        }

        JSONObject jsonObjectToPush = new JSONObject();
        try {
            jsonObjectToPush.put("type", "nearByCabs");
            JSONArray jsonArray = new JSONArray();
            for (LatLng location : nearbyCabLocations) {
                JSONObject jsonObjectLatLng = new JSONObject();
                jsonObjectLatLng.put("lat", location.lat);
                jsonObjectLatLng.put("lng", location.lng);
                jsonArray.put(jsonObjectLatLng);
            }
            jsonObjectToPush.put("locations", jsonArray);
            mainThread.post(() -> webSocketListener.onMessage(jsonObjectToPush.toString()));
        } catch (Exception e) {
            Log.e(TAG, "Error creating nearby cabs JSON", e);
        }
    }

    public static void requestCab(
            LatLng pickUpLocation,
            LatLng dropLocation,
            WebSocketListener webSocketListener
    ) {
        Simulator.pickUpLocation = pickUpLocation;
        Simulator.dropLocation = dropLocation;

        int randomOperatorForLat = random.nextInt(2);
        int randomOperatorForLng = random.nextInt(2);

        double randomDeltaForLat = (random.nextInt(26) + 5) / 10000.00; // (5..30).random()
        double randomDeltaForLng = (random.nextInt(26) + 5) / 10000.00;

        if (randomOperatorForLat == 1) {
            randomDeltaForLat *= -1;
        }
        if (randomOperatorForLng == 1) {
            randomDeltaForLng *= -1;
        }

        double latFakeNearby = Math.min(pickUpLocation.lat + randomDeltaForLat, 90.00);
        double lngFakeNearby = Math.min(pickUpLocation.lng + randomDeltaForLng, 180.00);

        LatLng bookedCabCurrentLocation = new LatLng(latFakeNearby, lngFakeNearby);
        DirectionsApiRequest directionsApiRequest = new DirectionsApiRequest(geoApiContext);
        directionsApiRequest.mode(TravelMode.DRIVING);
        directionsApiRequest.origin(bookedCabCurrentLocation);
        directionsApiRequest.destination(Simulator.pickUpLocation);
        directionsApiRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                Log.d(TAG, "onResult : " + result);
                try {
                    JSONObject jsonObjectCabBooked = new JSONObject();
                    jsonObjectCabBooked.put("type", "cabBooked");
                    mainThread.post(() -> webSocketListener.onMessage(jsonObjectCabBooked.toString()));

                    pickUpPath.clear();
                    DirectionsRoute[] routeList = result.routes;

                    if (routeList.length == 0) {
                        JSONObject jsonObjectFailure = new JSONObject();
                        jsonObjectFailure.put("type", "routesNotAvailable");
                        mainThread.post(() -> webSocketListener.onError(jsonObjectFailure.toString()));
                    } else {
                        for (DirectionsRoute route : routeList) {
                            List<LatLng> path = route.overviewPolyline.decodePath();
                            pickUpPath.addAll(path);
                        }

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("type", "pickUpPath");
                        JSONArray jsonArray = new JSONArray();
                        for (LatLng pickUp : pickUpPath) {
                            JSONObject jsonObjectLatLng = new JSONObject();
                            jsonObjectLatLng.put("lat", pickUp.lat);
                            jsonObjectLatLng.put("lng", pickUp.lng);
                            jsonArray.put(jsonObjectLatLng);
                        }
                        jsonObject.put("path", jsonArray);
                        mainThread.post(() -> webSocketListener.onMessage(jsonObject.toString()));

                        startTimerForPickUp(webSocketListener);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing directions result", e);
                }
            }

            @Override
            public void onFailure(Throwable e) {
                Log.d(TAG, "onFailure : " + e.getMessage());
                try {
                    JSONObject jsonObjectFailure = new JSONObject();
                    jsonObjectFailure.put("type", "directionApiFailed");
                    jsonObjectFailure.put("error", e.getMessage());
                    mainThread.post(() -> webSocketListener.onError(jsonObjectFailure.toString()));
                } catch (Exception ex) {
                    Log.e(TAG, "Error creating failure JSON", ex);
                }
            }
        });
    }

    public static void startTimerForPickUp(WebSocketListener webSocketListener) {
        long delay = 2000L;
        long period = 3000L;
        int size = pickUpPath.size();
        final int[] index = {0};
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "location");
                    jsonObject.put("lat", pickUpPath.get(index[0]).lat);
                    jsonObject.put("lng", pickUpPath.get(index[0]).lng);
                    mainThread.post(() -> webSocketListener.onMessage(jsonObject.toString()));

                    if (index[0] == size - 1) {
                        stopTimer();
                        JSONObject jsonObjectCabIsArriving = new JSONObject();
                        jsonObjectCabIsArriving.put("type", "cabIsArriving");
                        mainThread.post(() -> webSocketListener.onMessage(jsonObjectCabIsArriving.toString()));
                        startTimerForWaitDuringPickUp(webSocketListener);
                    }

                    index[0]++;
                } catch (Exception e) {
                    Log.e(TAG, "Error in pickup timer", e);
                }
            }
        };

        timer.schedule(timerTask, delay, period);
    }

    public static void startTimerForWaitDuringPickUp(WebSocketListener webSocketListener) {
        long delay = 3000L;
        long period = 3000L;
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                stopTimer();
                try {
                    JSONObject jsonObjectCabArrived = new JSONObject();
                    jsonObjectCabArrived.put("type", "cabArrived");
                    mainThread.post(() -> webSocketListener.onMessage(jsonObjectCabArrived.toString()));

                    DirectionsApiRequest directionsApiRequest = new DirectionsApiRequest(geoApiContext);
                    directionsApiRequest.mode(TravelMode.DRIVING);
                    directionsApiRequest.origin(pickUpLocation);
                    directionsApiRequest.destination(dropLocation);
                    directionsApiRequest.setCallback(new PendingResult.Callback<DirectionsResult>() {
                        @Override
                        public void onResult(DirectionsResult result) {
                            Log.d(TAG, "onResult : " + result);
                            try {
                                tripPath.clear();
                                DirectionsRoute[] routeList = result.routes;

                                if (routeList.length == 0) {
                                    JSONObject jsonObjectFailure = new JSONObject();
                                    jsonObjectFailure.put("type", "routesNotAvailable");
                                    mainThread.post(() -> webSocketListener.onError(jsonObjectFailure.toString()));
                                } else {
                                    for (DirectionsRoute route : routeList) {
                                        List<LatLng> path = route.overviewPolyline.decodePath();
                                        tripPath.addAll(path);
                                    }
                                    startTimerForTrip(webSocketListener);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing trip directions", e);
                            }
                        }

                        @Override
                        public void onFailure(Throwable e) {
                            Log.d(TAG, "onFailure : " + e.getMessage());
                            try {
                                JSONObject jsonObjectFailure = new JSONObject();
                                jsonObjectFailure.put("type", "directionApiFailed");
                                jsonObjectFailure.put("error", e.getMessage());
                                mainThread.post(() -> webSocketListener.onError(jsonObjectFailure.toString()));
                            } catch (Exception ex) {
                                Log.e(TAG, "Error creating failure JSON", ex);
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error in wait timer", e);
                }
            }
        };
        timer.schedule(timerTask, delay, period);
    }

    public static void startTimerForTrip(WebSocketListener webSocketListener) {
        long delay = 5000L;
        long period = 3000L;
        int size = tripPath.size();
        final int[] index = {0};
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                try {
                    if (index[0] == 0) {
                        JSONObject jsonObjectTripStart = new JSONObject();
                        jsonObjectTripStart.put("type", "tripStart");
                        mainThread.post(() -> webSocketListener.onMessage(jsonObjectTripStart.toString()));

                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("type", "tripPath");
                        JSONArray jsonArray = new JSONArray();
                        for (LatLng trip : tripPath) {
                            JSONObject jsonObjectLatLng = new JSONObject();
                            jsonObjectLatLng.put("lat", trip.lat);
                            jsonObjectLatLng.put("lng", trip.lng);
                            jsonArray.put(jsonObjectLatLng);
                        }
                        jsonObject.put("path", jsonArray);
                        mainThread.post(() -> webSocketListener.onMessage(jsonObject.toString()));
                    }

                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("type", "location");
                    jsonObject.put("lat", tripPath.get(index[0]).lat);
                    jsonObject.put("lng", tripPath.get(index[0]).lng);
                    mainThread.post(() -> webSocketListener.onMessage(jsonObject.toString()));

                    if (index[0] == size - 1) {
                        stopTimer();
                        startTimerForTripEndEvent(webSocketListener);
                    }

                    index[0]++;
                } catch (Exception e) {
                    Log.e(TAG, "Error in trip timer", e);
                }
            }
        };
        timer.schedule(timerTask, delay, period);
    }

    public static void startTimerForTripEndEvent(WebSocketListener webSocketListener) {
        long delay = 3000L;
        long period = 3000L;
        timer = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                stopTimer();
                try {
                    JSONObject jsonObjectTripEnd = new JSONObject();
                    jsonObjectTripEnd.put("type", "tripEnd");
                    mainThread.post(() -> webSocketListener.onMessage(jsonObjectTripEnd.toString()));
                } catch (Exception e) {
                    Log.e(TAG, "Error in trip end timer", e);
                }
            }
        };
        timer.schedule(timerTask, delay, period);
    }

    public static void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}