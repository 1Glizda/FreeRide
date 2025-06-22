package com.cristeabogdan.simulator;

import com.google.maps.model.LatLng;
import org.json.JSONObject;

public class WebSocket {
    private WebSocketListener webSocketListener;

    public WebSocket(WebSocketListener webSocketListener) {
        this.webSocketListener = webSocketListener;
    }

    public void connect() {
        webSocketListener.onConnect();
    }

    public void sendMessage(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            String type = jsonObject.getString("type");

            switch (type) {
                case "nearByCabs":
                    Simulator.getFakeNearbyCabLocations(
                            jsonObject.getDouble("lat"),
                            jsonObject.getDouble("lng"),
                            webSocketListener
                    );
                    break;
                case "requestCab":
                    LatLng pickUpLatLng = new LatLng(
                            jsonObject.getDouble("pickUpLat"),
                            jsonObject.getDouble("pickUpLng")
                    );
                    LatLng dropLatLng = new LatLng(
                            jsonObject.getDouble("dropLat"),
                            jsonObject.getDouble("dropLng")
                    );
                    Simulator.requestCab(
                            pickUpLatLng,
                            dropLatLng,
                            webSocketListener
                    );
                    break;
            }
        } catch (Exception e) {
            webSocketListener.onError("Error parsing message: " + e.getMessage());
        }
    }

    public void disconnect() {
        Simulator.stopTimer();
        this.webSocketListener.onDisconnect();
    }
}