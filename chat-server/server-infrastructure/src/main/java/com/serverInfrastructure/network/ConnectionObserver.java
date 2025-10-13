package com.serverInfrastructure.network;

public interface ConnectionObserver {
    void onClientConnected(ClientConnection connection);
    void onClientDisconnected(ClientConnection connection);
}
