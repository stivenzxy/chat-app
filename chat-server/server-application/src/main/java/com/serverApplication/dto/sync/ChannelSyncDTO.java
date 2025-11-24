package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record ChannelSyncDTO(
    int channelId,
    String name,
    String ownerId,
    String visibility,
    LocalDateTime createdAt
) {}
