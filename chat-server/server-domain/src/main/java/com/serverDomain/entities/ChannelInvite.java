package com.serverDomain.entities;

import java.time.LocalDateTime;

public class ChannelInvite {
    private final Integer id;
    private final Integer channelId;
    private final String inviterUserId;
    private final String invitedUserId;
    private final String inviterUsername;
    private final String channelName;
    private Status status;
    private final LocalDateTime createdAt;

    public enum Status { PENDING, ACCEPTED, REJECTED }

    public ChannelInvite(Integer id, Integer channelId, String inviterUserId, String invitedUserId,
                            Status status, LocalDateTime createdAt) {
        this(id, channelId, inviterUserId, invitedUserId, null, null, status, createdAt);
    }

    public ChannelInvite(Integer id, Integer channelId, String inviterUserId, String invitedUserId,
                            String inviterUsername, String channelName, Status status, LocalDateTime createdAt) {
        this.id = id;
        this.channelId = channelId;
        this.inviterUserId = inviterUserId;
        this.invitedUserId = invitedUserId;
        this.inviterUsername = inviterUsername;
        this.channelName = channelName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Integer getId() { return id; }
    public Integer getChannelId() { return channelId; }
    public String getInviterUserId() { return inviterUserId; }
    public String getInvitedUserId() { return invitedUserId; }
    public String getInviterUsername() { return inviterUsername; }
    public String getChannelName() { return channelName; }
    public Status getStatus() { return status; }
    public void accept() { this.status = Status.ACCEPTED; }
    public void reject() { this.status = Status.REJECTED; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}


