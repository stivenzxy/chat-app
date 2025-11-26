package com.serverDomain.entities;

import java.time.LocalDateTime;

public class Channel {
    private final String id;
    private final String name;
    private final String ownerId;
    private final Visibility visibility;
    private final LocalDateTime createdAt;

    public enum Visibility {
        PUBLIC, PRIVATE
    }

    public Channel(String id, String name, String ownerId, Visibility visibility, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.visibility = visibility;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
