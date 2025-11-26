package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record ChannelInviteSyncDTO(
        String inviteId,
        String channelId,
        String inviterUserId,
        String invitedUserId,
        String status,
        LocalDateTime createdAt) {
}
