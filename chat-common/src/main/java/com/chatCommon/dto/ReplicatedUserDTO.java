package com.chatCommon.dto;

import com.chatCommon.protocol.ProtocolParser;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Data Transfer Object for user replication across P2P servers.
 * Contains all necessary data to replicate a user in a remote server's database.
 */
public class ReplicatedUserDTO {
    
    private final String userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final String photoBase64; // can be empty
    private final String originServerId;
    
    public ReplicatedUserDTO(
            String userId,
            String username,
            String email,
            String passwordHash,
            String photoBase64,
            String originServerId) {
        
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.photoBase64 = photoBase64 != null ? photoBase64 : "";
        this.originServerId = originServerId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getPasswordHash() {
        return passwordHash;
    }
    
    public String getPhotoBase64() {
        return photoBase64;
    }
    
    public byte[] getPhotoData() {
        if (photoBase64 == null || photoBase64.isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(photoBase64);
        } catch (Exception e) {
            return null;
        }
    }
    
    public String getOriginServerId() {
        return originServerId;
    }
    
    /**
     * Converts to protocol message format.
     * Format: P2P_USER_REPLICATION|userId|username|email|passwordHash|photoBase64|originServerId
     */
    public String toProtocol() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        return parser.encode(
            "P2P_USER_REPLICATION",
            userId,
            username,
            email,
            passwordHash,
            photoBase64,
            originServerId
        );
    }
    
    /**
     * Parses from protocol message.
     */
    public static ReplicatedUserDTO fromProtocol(String message) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = parser.decode(message);
        
        if (parts.size() < 7) {
            throw new IllegalArgumentException("Invalid P2P_USER_REPLICATION message format");
        }
        
        return new ReplicatedUserDTO(
            parts.get(1), // userId
            parts.get(2), // username
            parts.get(3), // email
            parts.get(4), // passwordHash
            parts.get(5), // photoBase64
            parts.get(6)  // originServerId
        );
    }
    
    /**
     * Creates a batch replication message containing multiple users.
     * Format: P2P_BATCH_USER_REPLICATION|count|user1Data|user2Data|...
     */
    public static String toBatchProtocol(List<ReplicatedUserDTO> users, String originServerId) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = new ArrayList<>();
        parts.add("P2P_BATCH_USER_REPLICATION");
        parts.add(String.valueOf(users.size()));
        parts.add(originServerId);
        
        for (ReplicatedUserDTO user : users) {
            parts.add(user.getUserId());
            parts.add(user.getUsername());
            parts.add(user.getEmail());
            parts.add(user.getPasswordHash());
            parts.add(user.getPhotoBase64());
        }
        
        return parser.encode(parts.toArray(new String[0]));
    }
    
    /**
     * Parses batch replication message.
     */
    public static List<ReplicatedUserDTO> fromBatchProtocol(String message) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        List<String> parts = parser.decode(message);
        
        if (parts.size() < 3) {
            throw new IllegalArgumentException("Invalid P2P_BATCH_USER_REPLICATION message format");
        }
        
        int count = Integer.parseInt(parts.get(1));
        String originServerId = parts.get(2);
        List<ReplicatedUserDTO> users = new ArrayList<>();
        
        int index = 3;
        for (int i = 0; i < count; i++) {
            if (index + 4 >= parts.size()) {
                break;
            }
            
            users.add(new ReplicatedUserDTO(
                parts.get(index++),     // userId
                parts.get(index++),     // username
                parts.get(index++),     // email
                parts.get(index++),     // passwordHash
                parts.get(index++),     // photoBase64
                originServerId
            ));
        }
        
        return users;
    }
}
