package com.cristeabogdan.simulator;

public interface WebSocketListener {
    void onConnect();
    void onMessage(String data);
    void onDisconnect();
    void onError(String error);
}