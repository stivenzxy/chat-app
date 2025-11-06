package com.serverApplication.ports.peer;


@FunctionalInterface
public interface PeerMessageRouter {
    boolean routeToPeer(String recipientUsername, String peerMessage);
}
