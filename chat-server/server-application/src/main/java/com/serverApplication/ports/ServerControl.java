package com.serverApplication.ports;

public interface ServerControl {
    void startServer(int port);
    void stopServer();
    void addConnectionObserver(ClientConnectionObserver observer);
}
