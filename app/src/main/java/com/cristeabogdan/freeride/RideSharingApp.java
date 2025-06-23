package com.cristeabogdan.freeride;

import android.app.Application;
import android.util.Log;

import com.google.android.libraries.places.api.Places;
import com.google.maps.GeoApiContext;
import com.cristeabogdan.simulator.Simulator;
import com.cristeabogdan.freeride.database.MongoDBManager;

public class RideSharingApp extends Application {
    private static final String TAG = "RideSharingApp";

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Google services
        initializeGoogleServices();
        
        // Initialize MongoDB
        initializeMongoDB();
    }

    private void initializeGoogleServices() {
        try {
            // Get API key from resources
            String apiKey = getString(R.string.google_maps_key);

            // Verify API key exists
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalArgumentException("Google Maps API key is missing in strings.xml");
            }

            // Initialize Places SDK
            if (!Places.isInitialized()) {
                Places.initialize(getApplicationContext(), apiKey);
                Log.d(TAG, "Places SDK initialized successfully");
            }

            // Initialize GeoApiContext for simulator
            Simulator.geoApiContext = new GeoApiContext.Builder()
                    .apiKey(apiKey)
                    .build();

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Google services", e);
        }
    }

    private void initializeMongoDB() {
        try {
            // Initialize MongoDB connection
            MongoDBManager.getInstance();
            Log.d(TAG, "MongoDB manager initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize MongoDB", e);
        }
    }

    @Override
    public void onTerminate() {
        // Clean up resources
        if (Simulator.geoApiContext != null) {
            try {
                Simulator.geoApiContext.shutdown();
            } catch (Exception e) {
                Log.e(TAG, "Error shutting down GeoApiContext", e);
            }
        }
        
        // Close MongoDB connection
        try {
            MongoDBManager.getInstance().close();
        } catch (Exception e) {
            Log.e(TAG, "Error closing MongoDB connection", e);
        }
        
        super.onTerminate();
    }
}