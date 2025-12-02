package com.serverDomain.entities;

import com.serverDomain.exceptions.InvalidDomainException;
import com.serverDomain.services.PasswordHasher;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final String id;
    private final Username username;
    private final Email email;
    private String passwordHash;
    private byte[] photoData;
    private String ipAddress;
    private LocalDateTime createdAt;
    
    private boolean isReplicated;
    private String originServerId;
    private Timestamp lastSyncAt;

    public User(String id, Username username, Email email, String passwordHash, byte[] photoData, String ipAddress
    ,LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.photoData = photoData;
        this.ipAddress = ipAddress;
        this.createdAt = createdAt;
        this.isReplicated = false;
        this.originServerId = null;
        this.lastSyncAt = null;
    }

    public User(String id, String username, String email, String passwordHash) {
        this.id = id;
        this.username = new Username(username);
        this.email = new Email(email);
        this.passwordHash = passwordHash;
        this.photoData = null;
        this.ipAddress = null;
        this.createdAt = LocalDateTime.now();
        this.isReplicated = false;
        this.originServerId = null;
        this.lastSyncAt = null;
    }

    public static User create(Username username, Email email, String plainPassword,
                              byte[] photoData, String ipAddress, PasswordHasher hasher) {
        validatePassword(plainPassword);
        String passwordHash = hasher.hash(plainPassword);
        return new User(UUID.randomUUID().toString(), username, email, passwordHash, photoData, ipAddress, LocalDateTime.now());
    }


    public void changePassword(String newPlainPassword, PasswordHasher hasher) {
        validatePassword(newPlainPassword);
        this.passwordHash = hasher.hash(newPlainPassword);
    }

    public boolean verifyPassword(String plainPasswordToCheck, PasswordHasher hasher) {
        return hasher.check(plainPasswordToCheck, this.passwordHash);
    }

    public String getId() {
        return id;
    }

    public Username getUsername() {
        return username;
    }

    public Email getEmail() {
        return email;
    }

    public byte[] getPhotoData() {return photoData;}


    public String getIpAddress() {
        return ipAddress;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public boolean isReplicated() {
        return isReplicated;
    }
    
    public void setReplicated(boolean replicated) {
        this.isReplicated = replicated;
    }
    
    public String getOriginServerId() {
        return originServerId;
    }
    
    public void setOriginServerId(String originServerId) {
        this.originServerId = originServerId;
    }
    
    public Timestamp getLastSyncAt() {
        return lastSyncAt;
    }
    
    public void setLastSyncAt(Timestamp lastSyncAt) {
        this.lastSyncAt = lastSyncAt;
    }
    
    public void setPhotoData(byte[] photoData) {
        this.photoData = photoData;
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 6) {
            throw new InvalidDomainException("La contraseña debe tener al menos 6 caracteres");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof User user)) return false;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + id + '\'' +
                ", username=" + username +
                ", email=" + email +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}