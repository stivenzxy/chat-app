package com.serverApplication.dto.sync;

public record AudioTranscriptionSyncDTO(
        String messageId,
        String audioFormat,
        String transcribedText) {
}
