package com.serverApplication.ports;

public interface ServerControl {
    void startServer();
    void stopServer();
    void addConnectionObserver(ClientConnectionObserver observer);
    int getMaxConnections();
    int getCurrentConnections();
    int getServerPortNumber();
    void disconnectClient(String clientId);
    boolean sendBroadcastMessage(String message);
}
