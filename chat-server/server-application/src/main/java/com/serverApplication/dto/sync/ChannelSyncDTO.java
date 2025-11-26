package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record ChannelSyncDTO(
        String channelId,
        String name,
        String ownerId,
        String visibility,
        LocalDateTime createdAt) {
}
