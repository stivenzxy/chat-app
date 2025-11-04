package com.chatCommon.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class ChannelInviteDTO implements Serializable {
    private final Integer inviteId;
    private final Integer channelId;
    private final String inviterUserId;
    private final String inviterUsername;
    private final String invitedUserId;
    private final String channelName;
    private final InviteStatus status;
    private final LocalDateTime createdAt;

    public ChannelInviteDTO(Integer inviteId, Integer channelId, String inviterUserId, String inviterUsername,
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

    public Integer getInviteId() { return inviteId; }
    public Integer getChannelId() { return channelId; }
    public String getInviterUserId() { return inviterUserId; }
    public String getInviterUsername() { return inviterUsername; }
    public String getInvitedUserId() { return invitedUserId; }
    public String getChannelName() { return channelName; }
    public InviteStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}


