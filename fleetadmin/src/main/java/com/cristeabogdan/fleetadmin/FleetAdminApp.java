package com.cristeabogdan.fleetadmin;

import android.app.Application;
import android.util.Log;

import com.google.firebase.FirebaseApp;

public class FleetAdminApp extends Application {
    private static final String TAG = "FleetAdminApp";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize Firebase
        if (!FirebaseApp.getApps(this).isEmpty()) {
            Log.d(TAG, "Firebase already initialized");
        } else {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized");
        }
    }
}