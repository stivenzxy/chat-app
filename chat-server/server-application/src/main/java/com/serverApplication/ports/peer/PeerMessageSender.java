package com.serverApplication.ports.peer;


@FunctionalInterface
public interface PeerMessageSender {
    void sendToPeer(String peerId, String message);
}
