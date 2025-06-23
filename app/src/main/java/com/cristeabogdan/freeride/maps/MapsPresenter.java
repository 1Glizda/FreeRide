package com.cristeabogdan.freeride.maps;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.cristeabogdan.freeride.network.NetworkService;
import com.cristeabogdan.freeride.h3.CarManager;
import com.cristeabogdan.freeride.database.Car;
import com.cristeabogdan.freeride.database.MongoDBManager;
import com.cristeabogdan.simulator.WebSocket;
import com.cristeabogdan.simulator.WebSocketListener;
import com.cristeabogdan.freeride.utils.Constants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MapsPresenter implements WebSocketListener {

    private static final String TAG = "MapsPresenter";

    private final NetworkService networkService;
    private MapsView view;
    private WebSocket webSocket;
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
        if (webSocket != null) {
            webSocket.disconnect();
        }
        view = null;
    }

    public void requestNearbyCabs(LatLng latLng) {
        // Initialize cars if needed, then show available cars
        CarManager.initializeCarsIfNeeded(latLng.latitude, latLng.longitude, 
            new CarManager.CarInitializationCallback() {
                @Override
                public void onSuccess(List<Car> cars) {
                    Log.d(TAG, "Cars initialized successfully: " + cars.size());
                    showAvailableCars();
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Failed to initialize cars", e);
                    showAvailableCars(); // Try to show existing cars anyway
                }
            });
    }

    private void showAvailableCars() {
        CarManager.getAllAvailableCars(new MongoDBManager.CarQueryCallback() {
            @Override
            public void onSuccess(List<Car> cars) {
                List<LatLng> carLocations = new ArrayList<>();
                for (Car car : cars) {
                    carLocations.add(new LatLng(car.getLatitude(), car.getLongitude()));
                }
                
                if (view != null) {
                    view.showNearbyCabs(carLocations);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to get available cars", e);
            }
        });
    }

    public void requestCab(LatLng pickUpLatLng, LatLng dropLatLng) {
        // Use H3 algorithm to find nearest available car
        CarManager.findNearestAvailableCar(pickUpLatLng.latitude, pickUpLatLng.longitude,
            new CarManager.CarMatchingCallback() {
                @Override
                public void onCarMatched(Car car) {
                    Log.d(TAG, "Car matched: " + car.getCarId() + " - " + car.getDriverName());
                    assignedCarId = car.getCarId();
                    
                    // Inform view that cab is booked
                    if (view != null) {
                        view.informCabBooked();
                    }
                    
                    // Start simulation with the assigned car
                    LatLng carLocation = new LatLng(car.getLatitude(), car.getLongitude());
                    startSimulationWithAssignedCar(carLocation, pickUpLatLng, dropLatLng);
                }

                @Override
                public void onNoCarAvailable() {
                    Log.w(TAG, "No cars available in the area");
                    if (view != null) {
                        view.showDirectionApiFailedError("No cars available in your area. Please try again later.");
                    }
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Error finding car", e);
                    if (view != null) {
                        view.showDirectionApiFailedError("Error finding available car: " + e.getMessage());
                    }
                }
            });
    }

    private void startSimulationWithAssignedCar(LatLng carLocation, LatLng pickUpLatLng, LatLng dropLatLng) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "requestCab");
            jsonObject.put("pickUpLat", pickUpLatLng.latitude);
            jsonObject.put("pickUpLng", pickUpLatLng.longitude);
            jsonObject.put("dropLat", dropLatLng.latitude);
            jsonObject.put("dropLng", dropLatLng.longitude);
            jsonObject.put("carLat", carLocation.latitude);
            jsonObject.put("carLng", carLocation.longitude);
            jsonObject.put("assignedCarId", assignedCarId);
            
            webSocket.sendMessage(jsonObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for cab request", e);
        }
    }

    private void handleOnMessageNearbyCabs(JSONObject jsonObject) {
        try {
            List<LatLng> nearbyCabLocations = new ArrayList<>();
            JSONArray jsonArray = jsonObject.getJSONArray(Constants.LOCATIONS);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject locationObject = jsonArray.getJSONObject(i);
                double lat = locationObject.getDouble(Constants.LAT);
                double lng = locationObject.getDouble(Constants.LNG);
                LatLng latLng = new LatLng(lat, lng);
                nearbyCabLocations.add(latLng);
            }

            if (view != null) {
                view.showNearbyCabs(nearbyCabLocations);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing nearby cabs data", e);
        }
    }

    @Override
    public void onConnect() {
        Log.d(TAG, "onConnect");
    }

    @Override
    public void onMessage(String data) {
        Log.d(TAG, "onMessage data : " + data);
        try {
            JSONObject jsonObject = new JSONObject(data);
            String type = jsonObject.getString(Constants.TYPE);

            switch (type) {
                case Constants.NEAR_BY_CABS:
                    handleOnMessageNearbyCabs(jsonObject);
                    break;

                case Constants.CAB_BOOKED:
                    if (view != null) {
                        view.informCabBooked();
                    }
                    break;

                case Constants.PICKUP_PATH:
                case Constants.TRIP_PATH:
                    handlePathMessage(jsonObject);
                    break;

                case Constants.LOCATION:
                    handleLocationUpdate(jsonObject);
                    break;

                case Constants.CAB_IS_ARRIVING:
                    if (view != null) {
                        view.informCabIsArriving();
                    }
                    break;

                case Constants.CAB_ARRIVED:
                    if (view != null) {
                        view.informCabArrived();
                    }
                    break;

                case Constants.TRIP_START:
                    if (view != null) {
                        view.informTripStart();
                    }
                    break;

                case Constants.TRIP_END:
                    if (view != null) {
                        view.informTripEnd();
                    }
                    // Release the car after trip ends
                    if (assignedCarId != null) {
                        // Get final location from the last location update
                        double finalLat = jsonObject.optDouble("finalLat", 0);
                        double finalLng = jsonObject.optDouble("finalLng", 0);
                        
                        if (finalLat != 0 && finalLng != 0) {
                            CarManager.releaseCarAfterTrip(assignedCarId, finalLat, finalLng);
                        }
                        assignedCarId = null;
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown message type: " + type);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing message data", e);
        }
    }

    private void handlePathMessage(JSONObject jsonObject) {
        try {
            JSONArray jsonArray = jsonObject.getJSONArray("path");
            List<LatLng> pathPoints = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject pathObject = jsonArray.getJSONObject(i);
                double lat = pathObject.getDouble("lat");
                double lng = pathObject.getDouble("lng");
                LatLng latLng = new LatLng(lat, lng);
                pathPoints.add(latLng);
            }

            if (view != null) {
                view.showPath(pathPoints);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing path data", e);
        }
    }

    private void handleLocationUpdate(JSONObject jsonObject) {
        try {
            double latCurrent = jsonObject.getDouble("lat");
            double lngCurrent = jsonObject.getDouble("lng");

            if (view != null) {
                view.updateCabLocation(new LatLng(latCurrent, lngCurrent));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing location update", e);
        }
    }

    @Override
    public void onDisconnect() {
        Log.d(TAG, "onDisconnect");
    }

    @Override
    public void onError(String error) {
        Log.d(TAG, "onError : " + error);
        try {
            JSONObject jsonObject = new JSONObject(error);
            String type = jsonObject.getString(Constants.TYPE);

            switch (type) {
                case Constants.ROUTES_NOT_AVAILABLE:
                    if (view != null) {
                        view.showRoutesNotAvailableError();
                    }
                    break;

                case Constants.DIRECTION_API_FAILED:
                    if (view != null) {
                        String errorMessage = "Direction API Failed : " +
                                jsonObject.getString(Constants.ERROR);
                        view.showDirectionApiFailedError(errorMessage);
                    }
                    break;

                default:
                    Log.w(TAG, "Unknown error type: " + type);
                    break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing error data", e);
        }
    }
}