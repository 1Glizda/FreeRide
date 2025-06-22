package com.cristeabogdan.freeride.network;

import com.cristeabogdan.simulator.WebSocket;
import com.cristeabogdan.simulator.WebSocketListener;

public class NetworkService {

    public WebSocket createWebSocket(WebSocketListener webSocketListener) {
        return new WebSocket(webSocketListener);
    }
}