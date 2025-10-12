package com.serverApplication.dto;

public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String photoUrl;
    private String ipAddress;

    public CreateUserRequest(String username, String email, String password, String photoUrl, String ipAddress) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.photoUrl = photoUrl;
        this.ipAddress = ipAddress;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getPhotoUrl() { return photoUrl; }
    public String getIpAddress() { return ipAddress; }
}