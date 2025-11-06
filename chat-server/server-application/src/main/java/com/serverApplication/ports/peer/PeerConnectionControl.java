package com.serverApplication.ports.peer;

import com.serverApplication.dto.ConnectedPeerInfo;
import java.util.List;


public interface PeerConnectionControl {
    boolean startPeerServer();
    void stopPeerServer();
    boolean connectToPeer(String ip, int port);
    boolean disconnectFromPeer(String peerId);
    int disconnectFromPeers(List<String> peerIds);
    List<ConnectedPeerInfo> getConnectedPeers();
    boolean isPeerServerRunning();
    int getPeerServerPort();
    int getCurrentPeerConnections();
    boolean isConnectedToPeer(String peerId);
}