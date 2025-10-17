package com.serverApplication.dto;

public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private byte[] photoData;
    private String ipAddress;

    public CreateUserRequest(String username, String email, String password, byte[] photoData, String ipAddress) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.photoData = photoData;
        this.ipAddress = ipAddress;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public byte[] getPhotoData() { return photoData; }
    public String getIpAddress() { return ipAddress; }
}