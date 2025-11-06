package com.serverApplication.dto;

import java.time.LocalDateTime;

public class UserPresentationDTO {
    
    private final String id;
    private final String username;
    private final String email;
    private final String ipAddress;
    private final LocalDateTime createdAt;
    private byte[] photoData;
    
    public UserPresentationDTO(String id, String username, String email, String ipAddress, 
                              LocalDateTime createdAt, byte[] photoData) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.photoData = photoData;
    }
    
    public String getId() {
        return id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public byte[] getPhotoData() { return photoData; }
    
    @Override
    public String toString() {
        return "UserPresentationDTO{" +
                "id='" + id + '\'' +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}