package com.cristeabogdan.freeride.h3;

import android.util.Log;
import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;
import java.util.List;
import java.util.ArrayList;

public class H3Manager {
    private static final String TAG = "H3Manager";
    private static H3Core h3Core;
    private static final int H3_RESOLUTION = 9; // ~174m hexagon edge length

    static {
        try {
            h3Core = H3Core.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize H3Core", e);
        }
    }

    public static String getH3Index(double latitude, double longitude) {
        try {
            return h3Core.geoToH3(latitude, longitude, H3_RESOLUTION);
        } catch (Exception e) {
            Log.e(TAG, "Error converting coordinates to H3 index", e);
            return null;
        }
    }

    public static GeoCoord getH3Center(String h3Index) {
        try {
            return h3Core.h3ToGeo(h3Index);
        } catch (Exception e) {
            Log.e(TAG, "Error converting H3 index to coordinates", e);
            return null;
        }
    }

    public static List<String> getKRing(String h3Index, int k) {
        try {
            return h3Core.kRing(h3Index, k);
        } catch (Exception e) {
            Log.e(TAG, "Error getting k-ring", e);
            return new ArrayList<>();
        }
    }

    public static List<String> getNeighborH3Indices(double latitude, double longitude, int radius) {
        String centerH3 = getH3Index(latitude, longitude);
        if (centerH3 != null) {
            return getKRing(centerH3, radius);
        }
        return new ArrayList<>();
    }

    public static double getDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        try {
            GeoCoord coord1 = new GeoCoord(lat1, lon1);
            GeoCoord coord2 = new GeoCoord(lat2, lon2);
            return h3Core.pointDistKm(coord1, coord2);
        } catch (Exception e) {
            Log.e(TAG, "Error calculating distance", e);
            return Double.MAX_VALUE;
        }
    }
}