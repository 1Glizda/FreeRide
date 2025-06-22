package com.cristeabogdan.freeride.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import com.google.android.gms.maps.model.LatLng;
import com.cristeabogdan.freeride.R;

public class MapUtils {
    private static final String TAG = "MapUtils";

    public static Bitmap getCarBitmap(Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_car);
        return Bitmap.createScaledBitmap(bitmap, 50, 100, false);
    }

    public static Bitmap getDestinationBitmap() {
        int height = 20;
        int width = 20;
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        canvas.drawRect(0F, 0F, (float) width, (float) height, paint);
        return bitmap;
    }

    public static float getRotation(LatLng start, LatLng end) {
        double latDifference = Math.abs(start.latitude - end.latitude);
        double lngDifference = Math.abs(start.longitude - end.longitude);
        float rotation = -1F;

        if (start.latitude < end.latitude && start.longitude < end.longitude) {
            rotation = (float) Math.toDegrees(Math.atan(lngDifference / latDifference));
        } else if (start.latitude >= end.latitude && start.longitude < end.longitude) {
            rotation = (float) (90 - Math.toDegrees(Math.atan(lngDifference / latDifference)) + 90);
        } else if (start.latitude >= end.latitude && start.longitude >= end.longitude) {
            rotation = (float) (Math.toDegrees(Math.atan(lngDifference / latDifference)) + 180);
        } else if (start.latitude < end.latitude && start.longitude >= end.longitude) {
            rotation = (float) (90 - Math.toDegrees(Math.atan(lngDifference / latDifference)) + 270);
        }

        Log.d(TAG, "getRotation: " + rotation);
        return rotation;
    }
}