package com.cristeabogdan.freeride.maps;

import com.google.android.gms.maps.model.LatLng;
import java.util.List;

public interface MapsView {

    void showNearbyCabs(List<LatLng> latLngList);

    void informCabBooked();

    void showPath(List<LatLng> latLngList);

    void updateCabLocation(LatLng latLng);

    void informCabIsArriving();

    void informCabArrived();

    void informTripStart();

    void informTripEnd();

    void showRoutesNotAvailableError();

    void showDirectionApiFailedError(String error);
}