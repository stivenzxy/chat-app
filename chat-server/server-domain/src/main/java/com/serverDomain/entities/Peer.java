package com.serverDomain.entities;

import java.sql.Timestamp;

public class Peer {
    
    private final String peerId;
    private final String ipAddress;
    private final int port;
    private Timestamp lastSeenAt;
    private boolean isActive;
    private String discoveredFrom;
    private final Timestamp createdAt;

    public Peer(String peerId, String ipAddress, int port) {
        this.peerId = peerId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastSeenAt = new Timestamp(System.currentTimeMillis());
        this.isActive = true;
        this.discoveredFrom = null;
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    public Peer(String peerId, String ipAddress, int port, Timestamp lastSeenAt, 
                boolean isActive, String discoveredFrom, Timestamp createdAt) {
        this.peerId = peerId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.lastSeenAt = lastSeenAt;
        this.isActive = isActive;
        this.discoveredFrom = discoveredFrom;
        this.createdAt = createdAt;
    }

    public String getPeerId() {
        return peerId;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public int getPort() {
        return port;
    }
    
    public Timestamp getLastSeenAt() {
        return lastSeenAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public String getDiscoveredFrom() {
        return discoveredFrom;
    }
    
    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setLastSeenAt(Timestamp lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public void setDiscoveredFrom(String discoveredFrom) {
        this.discoveredFrom = discoveredFrom;
    }

    public void updateLastSeen() {
        this.lastSeenAt = new Timestamp(System.currentTimeMillis());
    }

    public void markActive() {
        this.isActive = true;
        updateLastSeen();
    }
    
    @Override
    public String toString() {
        return String.format("Peer{id='%s', ip='%s', port=%d, active=%s, lastSeen=%s}",
                peerId, ipAddress, port, isActive, lastSeenAt);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Peer peer = (Peer) obj;
        return peerId.equals(peer.peerId);
    }
    
    @Override
    public int hashCode() {
        return peerId.hashCode();
    }
}