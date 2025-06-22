package com.cristeabogdan.freeride.maps;

import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.cristeabogdan.freeride.network.NetworkService;
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
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(Constants.TYPE, Constants.NEAR_BY_CABS);
            jsonObject.put(Constants.LAT, latLng.latitude);
            jsonObject.put(Constants.LNG, latLng.longitude);
            webSocket.sendMessage(jsonObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON for nearby cabs request", e);
        }
    }

    public void requestCab(LatLng pickUpLatLng, LatLng dropLatLng) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "requestCab");
            jsonObject.put("pickUpLat", pickUpLatLng.latitude);
            jsonObject.put("pickUpLng", pickUpLatLng.longitude);
            jsonObject.put("dropLat", dropLatLng.latitude);
            jsonObject.put("dropLng", dropLatLng.longitude);
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