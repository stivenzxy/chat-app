package com.serverApplication.ports.peer;

public interface PeerNetworkControl extends
        PeerConnectionControl,
        PeerUserSyncControl,
        PeerMessagingControl,
        PeerObservableControl {
}