package com.serverApplication.dto.sync;

import java.time.LocalDateTime;

public record UserSyncDTO(
    String userId,
    String username,
    String email,
    String passwordHash,
    byte[] photoData,
    String ipAddress,
    boolean isReplicated,
    String originServerId,
    LocalDateTime lastSyncAt,
    LocalDateTime createdAt
) {}
