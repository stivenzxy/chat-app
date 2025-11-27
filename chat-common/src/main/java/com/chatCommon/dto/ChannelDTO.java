package com.chatCommon.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChannelDTO implements Serializable {
    private final String id;
    private final String name;
    private final String ownerId;
    private final ChannelVisibility visibility;
    private final LocalDateTime createdAt;

    public ChannelDTO(String id, String name, String ownerId, ChannelVisibility visibility, LocalDateTime createdAt) {
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

    public ChannelVisibility getVisibility() {
        return visibility;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
