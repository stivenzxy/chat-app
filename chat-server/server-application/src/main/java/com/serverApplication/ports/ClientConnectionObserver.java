package com.serverApplication.ports;

import com.serverApplication.dto.ConnectedClientInfo;

public interface ClientConnectionObserver {
    void onClientConnected(ConnectedClientInfo clientInfo);
    void onClientDisconnected(ConnectedClientInfo clientInfo);
}
