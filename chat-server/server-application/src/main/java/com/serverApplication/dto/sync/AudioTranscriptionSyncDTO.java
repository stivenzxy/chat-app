package com.serverApplication.dto.sync;

public record AudioTranscriptionSyncDTO(
    int messageId,
    String audioFormat,
    String transcribedText
) {}
