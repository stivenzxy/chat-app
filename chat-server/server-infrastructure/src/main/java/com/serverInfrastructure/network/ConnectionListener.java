package com.serverInfrastructure.network;


public interface ConnectionListener {
    void onClientConnected(ClientConnection connection);
    void onClientDisconnected(ClientConnection connection);
}