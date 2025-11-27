package com.chatCommon.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChannelInviteDTO implements Serializable {
    private final String inviteId;
    private final String channelId;
    private final String inviterUserId;
    private final String inviterUsername;
    private final String invitedUserId;
    private final String channelName;
    private final InviteStatus status;
    private final LocalDateTime createdAt;

    public ChannelInviteDTO(String inviteId, String channelId, String inviterUserId, String inviterUsername,
            String invitedUserId, String channelName, InviteStatus status, LocalDateTime createdAt) {
        this.inviteId = inviteId;
        this.channelId = channelId;
        this.inviterUserId = inviterUserId;
        this.inviterUsername = inviterUsername;
        this.invitedUserId = invitedUserId;
        this.channelName = channelName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getInviteId() {
        return inviteId;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getInviterUserId() {
        return inviterUserId;
    }

    public String getInviterUsername() {
        return inviterUsername;
    }

    public String getInvitedUserId() {
        return invitedUserId;
    }

    public String getChannelName() {
        return channelName;
    }

    public InviteStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
