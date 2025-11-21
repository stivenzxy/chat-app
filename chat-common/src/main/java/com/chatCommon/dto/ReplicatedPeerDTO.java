package com.chatCommon.dto;

import com.chatCommon.protocol.ProtocolParser;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for peer discovery replication across P2P servers.
 * Enables transitive peer discovery where peers share their known peers.
 * 
 * Example: ServerA connects to ServerB
 *          ServerB shares its known peers (ServerC, ServerD)
 *          ServerA automatically connects to ServerC and ServerD
 */
public class ReplicatedPeerDTO {
    
    private final String peerId;
    private final String ipAddress;
    private final int port;
    private final boolean isActive;
    
    public ReplicatedPeerDTO(String peerId, String ipAddress, int port, boolean isActive) {
        this.peerId = peerId;
        this.ipAddress = ipAddress;
        this.port = port;
        this.isActive = isActive;
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
    
    public boolean isActive() {
        return isActive;
    }
    
    /**
     * Converts to protocol message format.
     * Format: P2P_PEER_DISCOVERY|peerId|ipAddress|port|isActive
     */
    public String toProtocol() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        return parser.encode(
            "P2P_PEER_DISCOVERY",
            peerId,
            ipAddress,
            String.valueOf(port),
            String.valueOf(isActive)
        );
    }
    
    /**
     * Parses from protocol message.
     */
    public static ReplicatedPeerDTO fromProtocol(String message) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = parser.decode(message);
        
        if (parts.size() < 5) {
            throw new IllegalArgumentException("Invalid P2P_PEER_DISCOVERY message format");
        }
        
        return new ReplicatedPeerDTO(
            parts.get(1), // peerId
            parts.get(2), // ipAddress
            Integer.parseInt(parts.get(3)), // port
            Boolean.parseBoolean(parts.get(4)) // isActive
        );
    }
    
    /**
     * Creates a batch peer discovery message containing multiple peers.
     * Format: P2P_BATCH_PEER_DISCOVERY|count|sourcePeerId|peer1Data|peer2Data|...
     */
    public static String toBatchProtocol(List<ReplicatedPeerDTO> peers, String sourcePeerId) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = new ArrayList<>();
        parts.add("P2P_BATCH_PEER_DISCOVERY");
        parts.add(String.valueOf(peers.size()));
        parts.add(sourcePeerId);
        
        for (ReplicatedPeerDTO peer : peers) {
            parts.add(peer.getPeerId());
            parts.add(peer.getIpAddress());
            parts.add(String.valueOf(peer.getPort()));
            parts.add(String.valueOf(peer.isActive()));
        }
        
        return parser.encode(parts.toArray(new String[0]));
    }
    
    /**
     * Parses batch peer discovery message.
     */
    public static List<ReplicatedPeerDTO> fromBatchProtocol(String message) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = parser.decode(message);
        
        if (parts.size() < 3) {
            throw new IllegalArgumentException("Invalid P2P_BATCH_PEER_DISCOVERY message format");
        }
        
        int count = Integer.parseInt(parts.get(1));
        String sourcePeerId = parts.get(2);
        List<ReplicatedPeerDTO> peers = new ArrayList<>();
        
        int index = 3;
        for (int i = 0; i < count; i++) {
            if (index + 3 >= parts.size()) {
                break;
            }
            
            peers.add(new ReplicatedPeerDTO(
                parts.get(index++),     // peerId
                parts.get(index++),     // ipAddress
                Integer.parseInt(parts.get(index++)), // port
                Boolean.parseBoolean(parts.get(index++)) // isActive
            ));
        }
        
        return peers;
    }
    
    /**
     * Extracts source peer ID from batch message without full parsing.
     */
    public static String extractSourcePeerId(String batchMessage) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = parser.decode(batchMessage);
        
        if (parts.size() >= 3) {
            return parts.get(2); // sourcePeerId is at index 2
        }
        return null;
    }
}
