package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record MessageSyncDTO(
        String messageId,
        String authorId,
        String recipientUserId,
        String recipientChannelId,
        String content,
        String messageType,
        byte[] audioContent,
        LocalDateTime createdAt) {
}
