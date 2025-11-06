package com.serverApplication.ports.peer;

import com.serverApplication.ports.PeerConnectionObserver;

public interface PeerObservableControl {
    void addPeerConnectionObserver(PeerConnectionObserver observer);
    void removePeerConnectionObserver(PeerConnectionObserver observer);
}