package com.cristeabogdan.freeride;

import android.app.Application;
import android.util.Log;

import com.cristeabogdan.freeride.database.FirestoreManager;
import com.cristeabogdan.simulator.Simulator;
import com.google.android.libraries.places.api.Places;
import com.google.firebase.FirebaseApp;
import com.google.maps.GeoApiContext;

public class RideSharingApp extends Application {
    private static final String TAG = "RideSharingApp";

    @Override
    public void onCreate() {
        super.onCreate();
        System.loadLibrary("h3-java");
        initializeGoogleServices();
        FirestoreManager.init();
    }

    private void initializeGoogleServices() {
        try {
            String apiKey = getString(R.string.google_maps_key);
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("Missing Google Maps API key");
            }
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);
                Log.d(TAG, "Places SDK initialized");
            }
            Simulator.geoApiContext = new GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build();
        } catch (Exception e) {
            Log.e(TAG, "Google services init failed", e);
        }
    }

    @Override
    public void onTerminate() {
        if (Simulator.geoApiContext != null) {
            try { Simulator.geoApiContext.shutdown(); }
            catch (Exception e) { Log.e(TAG, "GeoApiContext shutdown failed", e); }
        }
        super.onTerminate();
    }
}
