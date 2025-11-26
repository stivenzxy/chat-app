package com.serverApplication.dto;

import java.time.LocalDateTime;

/**
 * DTO for user information in REST API responses.
 * Does not include sensitive data like passwords or large binary data (photos).
 */
public class UserResponseDTO {
    private final String id;
    private final String username;
    private final String email;
    private final String ipAddress;
    private final LocalDateTime createdAt;

    public UserResponseDTO(String id, String username, String email, String ipAddress, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
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
}
