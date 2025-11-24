package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record MessageSyncDTO(
    int messageId,
    String authorId,
    String recipientUserId,
    Integer recipientChannelId,
    String content,
    String messageType,
    byte[] audioContent,
    LocalDateTime createdAt
) {}
