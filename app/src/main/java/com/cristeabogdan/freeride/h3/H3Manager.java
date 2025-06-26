package com.cristeabogdan.freeride.h3;

import android.util.Log;

import com.uber.h3core.H3Core;
import com.uber.h3core.util.GeoCoord;

import java.util.ArrayList;
import java.util.List;

public class H3Manager {
    private static final String TAG = "H3Manager";
    private static H3Core h3Core;
    public static final int H3_RESOLUTION = 5;

    static {
        h3Core = H3Core.newSystemInstance();
    }

    /** Returns the H3 index as a long. */
    public static long getH3Index(double lat, double lng, int i) {
        try {
            return h3Core.geoToH3(lat, lng, i);
        } catch (Exception e) {
            Log.e(TAG, "geoToH3 error", e);
            return -1;
        }
    }

    /** Returns the [lat, lng] center of the given H3 index as a GeoCoord. */
    public static GeoCoord getH3Center(long idx) {
        try {
            return h3Core.h3ToGeo(idx);  // renamed in 3.x :contentReference[oaicite:1]{index=1}
        } catch (Exception e) {
            Log.e(TAG, "h3ToGeo error", e);
            return null;
        }
    }

    /** Returns the k-ring of indices around the given cell. */
    public static List<Long> getKRing(long idx, int k) {
        try {
            return h3Core.kRing(idx, k);  // renamed in 3.x :contentReference[oaicite:2]{index=2}
        } catch (Exception e) {
            Log.e(TAG, "kRing error", e);
            return new ArrayList<>();
        }
    }

    /** Convenience: all indices within radius around a lat/lng. */
    public static List<Long> getNeighborH3Indices(double lat, double lng, int ring) {
        long center = getH3Index(lat, lng, 5);
        if (center < 0) {
            Log.e(TAG, "Invalid H3 index generated");
            return new ArrayList<>();
        }
        return getKRing(center, ring);
    }

    /** Haversine distance in kilometers. */
    public static double getDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        try {
            double dLat = Math.toRadians(lat2 - lat1);
            double dLon = Math.toRadians(lon2 - lon1);
            double a = Math.sin(dLat/2)*Math.sin(dLat/2)
                    + Math.cos(Math.toRadians(lat1))*Math.cos(Math.toRadians(lat2))
                    * Math.sin(dLon/2)*Math.sin(dLon/2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            return R * c;
        } catch (Exception e) {
            Log.e(TAG, "distance calc error", e);
            return Double.MAX_VALUE;
        }
    }
}
