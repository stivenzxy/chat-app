package com.serverApplication.ports;

import com.serverApplication.dto.ConnectedPeerInfo;

public interface PeerConnectionObserver {
    void onPeerConnected(ConnectedPeerInfo peerInfo);
    void onPeerDisconnected(ConnectedPeerInfo peerInfo);
    void onPeerStatusChanged(ConnectedPeerInfo peerInfo);
    void onPeerConnectionError(String peerId, String errorMessage);
}