package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record ChannelInviteSyncDTO(
    int inviteId,
    int channelId,
    String inviterUserId,
    String invitedUserId,
    String status,
    LocalDateTime createdAt
) {}
