package com.serverApplication.ports.peer;

public interface PeerMessagingControl {
    boolean sendMessageToPeer(String peerId, String message);
    int broadcastToPeers(String message);
}