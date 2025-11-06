package com.serverApplication.dto;

import java.time.LocalDateTime;

public record ConnectedPeerInfo(
    String peerId,
    String ipAddress,
    int port,
    String status,
    LocalDateTime connectionTime,
    int latency,
    int connectedUsers,
    int availableChannels
) {

    public static ConnectedPeerInfo create(String ipAddress, int port, String status) {
        String peerId = ipAddress + ":" + port;
        return new ConnectedPeerInfo(
            peerId,
            ipAddress,
            port,
            status,
            LocalDateTime.now(),
            -1,
            0,
            0
        );
    }

    public ConnectedPeerInfo withStatus(String newStatus) {
        return new ConnectedPeerInfo(
            peerId, ipAddress, port, newStatus, connectionTime, latency, connectedUsers, availableChannels
        );
    }

    public ConnectedPeerInfo withLatency(int newLatency) {
        return new ConnectedPeerInfo(
            peerId, ipAddress, port, status, connectionTime, newLatency, connectedUsers, availableChannels
        );
    }

    public ConnectedPeerInfo withCounters(int users, int channels) {
        return new ConnectedPeerInfo(
            peerId, ipAddress, port, status, connectionTime, latency, users, channels
        );
    }

    public boolean isConnected() {
        return "Conectado".equals(status);
    }

    public String getDisplayName() {
        return String.format("%s:%d (%s)", ipAddress, port, status);
    }
}