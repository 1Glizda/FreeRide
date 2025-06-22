package com.cristeabogdan.freeride.maps;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.cristeabogdan.freeride.R;
import com.cristeabogdan.freeride.databinding.ActivityMapsBinding;
import com.cristeabogdan.freeride.network.NetworkService;
import com.cristeabogdan.freeride.utils.AnimationUtils;
import com.cristeabogdan.freeride.utils.MapUtils;
import com.cristeabogdan.freeride.utils.PermissionUtils;
import com.cristeabogdan.freeride.utils.ViewUtils;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements MapsView, OnMapReadyCallback {

    private static final String TAG = "MapsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 999;
    private static final int PICKUP_REQUEST_CODE = 1;
    private static final int DROP_REQUEST_CODE = 2;

    private ActivityMapsBinding binding;
    private MapsPresenter presenter;
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LatLng currentLatLng;
    private LatLng pickUpLatLng;
    private LatLng dropLatLng;
    private ArrayList<Marker> nearbyCabMarkerList = new ArrayList<>();
    private Marker destinationMarker;
    private Marker originMarker;
    private Polyline greyPolyLine;
    private Polyline blackPolyline;
    private LatLng previousLatLngFromServer;
    private LatLng currentLatLngFromServer;
    private Marker movingCabMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewUtils.enableTransparentStatusBar(getWindow());
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        presenter = new MapsPresenter(new NetworkService());
        presenter.onAttach(this);
        setUpClickListener();
    }

    private void setUpClickListener() {
        binding.pickUpTextView.setOnClickListener(v ->
                launchLocationAutoCompleteActivity(PICKUP_REQUEST_CODE));

        binding.dropTextView.setOnClickListener(v ->
                launchLocationAutoCompleteActivity(DROP_REQUEST_CODE));

        binding.requestCabButton.setOnClickListener(v -> {
            binding.statusTextView.setVisibility(View.VISIBLE);
            binding.statusTextView.setText(getString(R.string.requesting_your_cab));
            binding.requestCabButton.setEnabled(false);
            binding.pickUpTextView.setEnabled(false);
            binding.dropTextView.setEnabled(false);
            presenter.requestCab(pickUpLatLng, dropLatLng);
        });

        binding.nextRideButton.setOnClickListener(v -> reset());
    }

    private void launchLocationAutoCompleteActivity(int requestCode) {
        List<Place.Field> fields = Arrays.asList(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
        );
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .build(this);
        startActivityForResult(intent, requestCode);
    }

    private void moveCamera(LatLng latLng) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    private void animateCamera(LatLng latLng) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(latLng)
                .zoom(15.5f)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    private Marker addCarMarkerAndGet(LatLng latLng) {
        return googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(MapUtils.getCarBitmap(this))));
    }

    private Marker addOriginDestinationMarkerAndGet(LatLng latLng) {
        return googleMap.addMarker(new MarkerOptions()
                .position(latLng)
                .flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(MapUtils.getDestinationBitmap())));
    }

    private void setCurrentLocationAsPickUp() {
        pickUpLatLng = currentLatLng;
        binding.pickUpTextView.setText(getString(R.string.current_location));
    }

    private void enableMyLocationOnMap() {
        googleMap.setPadding(0, ViewUtils.dpToPx(48f), 0, 0);
        try {
            googleMap.setMyLocationEnabled(true);
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }

    private void setUpLocationListener() {
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                if (currentLatLng == null) {
                    if (locationResult.getLocations() != null) {
                        for (android.location.Location location : locationResult.getLocations()) {
                            if (currentLatLng == null) {
                                currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                                setCurrentLocationAsPickUp();
                                enableMyLocationOnMap();
                                moveCamera(currentLatLng);
                                animateCamera(currentLatLng);
                                presenter.requestNearbyCabs(currentLatLng);
                            }
                        }
                    }
                }
                // Few more things we can do here:
                // For example: Update the location of user on server
            }
        };

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
            );
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission not granted", e);
        }
    }

    private void checkAndShowRequestButton() {
        if (pickUpLatLng != null && dropLatLng != null) {
            binding.requestCabButton.setVisibility(View.VISIBLE);
            binding.requestCabButton.setEnabled(true);
        }
    }

    private void reset() {
        binding.statusTextView.setVisibility(View.GONE);
        binding.nextRideButton.setVisibility(View.GONE);

        for (Marker marker : nearbyCabMarkerList) {
            marker.remove();
        }
        nearbyCabMarkerList.clear();

        previousLatLngFromServer = null;
        currentLatLngFromServer = null;

        if (currentLatLng != null) {
            moveCamera(currentLatLng);
            animateCamera(currentLatLng);
            setCurrentLocationAsPickUp();
            presenter.requestNearbyCabs(currentLatLng);
        } else {
            binding.pickUpTextView.setText("");
        }

        binding.pickUpTextView.setEnabled(true);
        binding.dropTextView.setEnabled(true);
        binding.dropTextView.setText("");

        if (movingCabMarker != null) {
            movingCabMarker.remove();
        }
        if (greyPolyLine != null) {
            greyPolyLine.remove();
        }
        if (blackPolyline != null) {
            blackPolyline.remove();
        }
        if (originMarker != null) {
            originMarker.remove();
        }
        if (destinationMarker != null) {
            destinationMarker.remove();
        }

        dropLatLng = null;
        greyPolyLine = null;
        blackPolyline = null;
        originMarker = null;
        destinationMarker = null;
        movingCabMarker = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (currentLatLng == null) {
            if (PermissionUtils.isAccessFineLocationGranted(this)) {
                if (PermissionUtils.isLocationEnabled(this)) {
                    setUpLocationListener();
                } else {
                    PermissionUtils.showGPSNotEnabledDialog(this);
                }
            } else {
                PermissionUtils.requestAccessFineLocationPermission(
                        this,
                        LOCATION_PERMISSION_REQUEST_CODE
                );
            }
        }
    }

    @Override
    protected void onDestroy() {
        presenter.onDetach();
        if (fusedLocationProviderClient != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (PermissionUtils.isLocationEnabled(this)) {
                    setUpLocationListener();
                } else {
                    PermissionUtils.showGPSNotEnabledDialog(this);
                }
            } else {
                Toast.makeText(
                        this,
                        getString(R.string.location_permission_not_granted),
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICKUP_REQUEST_CODE || requestCode == DROP_REQUEST_CODE) {
            switch (resultCode) {
                case RESULT_OK:
                    Place place = Autocomplete.getPlaceFromIntent(data);
                    Log.d(TAG, "Place: " + place.getName() + ", " + place.getId() + ", " + place.getLatLng());
                    if (requestCode == PICKUP_REQUEST_CODE) {
                        binding.pickUpTextView.setText(place.getName());
                        pickUpLatLng = place.getLatLng();
                        checkAndShowRequestButton();
                    } else if (requestCode == DROP_REQUEST_CODE) {
                        binding.dropTextView.setText(place.getName());
                        dropLatLng = place.getLatLng();
                        checkAndShowRequestButton();
                    }
                    break;

                case AutocompleteActivity.RESULT_ERROR:
                    Status status = Autocomplete.getStatusFromIntent(data);
                    Log.d(TAG, status.getStatusMessage());
                    break;

                case RESULT_CANCELED:
                    Log.d(TAG, "Place Selection Canceled");
                    break;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
    }

    @Override
    public void showNearbyCabs(List<LatLng> latLngList) {
        nearbyCabMarkerList.clear();
        for (LatLng latLng : latLngList) {
            Marker nearbyCabMarker = addCarMarkerAndGet(latLng);
            if (nearbyCabMarker != null) {
                nearbyCabMarkerList.add(nearbyCabMarker);
            }
        }
    }

    @Override
    public void informCabBooked() {
        for (Marker marker : nearbyCabMarkerList) {
            marker.remove();
        }
        nearbyCabMarkerList.clear();
        binding.requestCabButton.setVisibility(View.GONE);
        binding.statusTextView.setText(getString(R.string.your_cab_is_booked));
    }

    @Override
    public void showPath(List<LatLng> latLngList) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : latLngList) {
            builder.include(latLng);
        }
        LatLngBounds bounds = builder.build();
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 2));

        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.GRAY)
                .width(5f)
                .addAll(latLngList);
        greyPolyLine = googleMap.addPolyline(polylineOptions);

        PolylineOptions blackPolylineOptions = new PolylineOptions()
                .width(5f)
                .color(Color.BLACK);
        blackPolyline = googleMap.addPolyline(blackPolylineOptions);

        originMarker = addOriginDestinationMarkerAndGet(latLngList.get(0));
        if (originMarker != null) {
            originMarker.setAnchor(0.5f, 0.5f);
        }

        destinationMarker = addOriginDestinationMarkerAndGet(latLngList.get(latLngList.size() - 1));
        if (destinationMarker != null) {
            destinationMarker.setAnchor(0.5f, 0.5f);
        }

        ValueAnimator polylineAnimator = AnimationUtils.polyLineAnimator();
        polylineAnimator.addUpdateListener(valueAnimator -> {
            int percentValue = (Integer) valueAnimator.getAnimatedValue();
            int index = (int) (greyPolyLine.getPoints().size() * (percentValue / 100.0f));
            if (blackPolyline != null && greyPolyLine != null) {
                blackPolyline.setPoints(greyPolyLine.getPoints().subList(0, index));
            }
        });
        polylineAnimator.start();
    }

    @Override
    public void updateCabLocation(LatLng latLng) {
        if (movingCabMarker == null) {
            movingCabMarker = addCarMarkerAndGet(latLng);
        }

        if (previousLatLngFromServer == null) {
            currentLatLngFromServer = latLng;
            previousLatLngFromServer = currentLatLngFromServer;
            if (movingCabMarker != null) {
                movingCabMarker.setPosition(currentLatLngFromServer);
                movingCabMarker.setAnchor(0.5f, 0.5f);
            }
            animateCamera(currentLatLngFromServer);
        } else {
            previousLatLngFromServer = currentLatLngFromServer;
            currentLatLngFromServer = latLng;

            ValueAnimator valueAnimator = AnimationUtils.cabAnimator();
            valueAnimator.addUpdateListener(va -> {
                if (currentLatLngFromServer != null && previousLatLngFromServer != null) {
                    float multiplier = va.getAnimatedFraction();
                    LatLng nextLocation = new LatLng(
                            multiplier * currentLatLngFromServer.latitude + (1 - multiplier) * previousLatLngFromServer.latitude,
                            multiplier * currentLatLngFromServer.longitude + (1 - multiplier) * previousLatLngFromServer.longitude
                    );

                    if (movingCabMarker != null) {
                        movingCabMarker.setPosition(nextLocation);
                        movingCabMarker.setAnchor(0.5f, 0.5f);

                        float rotation = MapUtils.getRotation(previousLatLngFromServer, nextLocation);
                        if (!Float.isNaN(rotation)) {
                            movingCabMarker.setRotation(rotation);
                        }
                    }
                    animateCamera(nextLocation);
                }
            });
            valueAnimator.start();
        }
    }

    @Override
    public void informCabIsArriving() {
        binding.statusTextView.setText(getString(R.string.your_cab_is_arriving));
    }

    @Override
    public void informCabArrived() {
        binding.statusTextView.setText(getString(R.string.your_cab_has_arrived));
        if (greyPolyLine != null) {
            greyPolyLine.remove();
        }
        if (blackPolyline != null) {
            blackPolyline.remove();
        }
        if (originMarker != null) {
            originMarker.remove();
        }
        if (destinationMarker != null) {
            destinationMarker.remove();
        }
    }

    @Override
    public void informTripStart() {
        binding.statusTextView.setText(getString(R.string.you_are_on_a_trip));
        previousLatLngFromServer = null;
    }

    @Override
    public void informTripEnd() {
        binding.statusTextView.setText(getString(R.string.trip_end));
        binding.nextRideButton.setVisibility(View.VISIBLE);
        if (greyPolyLine != null) {
            greyPolyLine.remove();
        }
        if (blackPolyline != null) {
            blackPolyline.remove();
        }
        if (originMarker != null) {
            originMarker.remove();
        }
        if (destinationMarker != null) {
            destinationMarker.remove();
        }
    }

    @Override
    public void showRoutesNotAvailableError() {
        String error = getString(R.string.route_not_available_choose_different_locations);
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        reset();
    }

    @Override
    public void showDirectionApiFailedError(String error) {
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        reset();
    }
}