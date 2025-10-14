package com.serverApplication.dto;

import java.time.LocalDateTime;

/**
 * DTO para representar un usuario en las capas superiores (presentación)
 */
public class UserPresentationDTO {
    
    private final String id;
    private final String username;
    private final String email;
    private final String ipAddress;
    private final LocalDateTime createdAt;
    private final String photoUrl;
    
    public UserPresentationDTO(String id, String username, String email, String ipAddress, 
                              LocalDateTime createdAt, String photoUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.photoUrl = photoUrl;
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
    
    public String getPhotoUrl() {
        return photoUrl;
    }
    
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