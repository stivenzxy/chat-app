package com.serverApplication.ports;

import com.serverApplication.dto.ClientConnection;

public interface ConnectionObserver {
    void onClientConnected(ClientConnection connection);
    void onClientDisconnected(ClientConnection connection);
}
