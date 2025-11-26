package com.serverApplication.dto;

import java.time.LocalDateTime;

public class LogEntryDTO {
    private final String level;
    private final String message;
    private final LocalDateTime timestamp;
    private final String source;

    public LogEntryDTO(String level, String message, LocalDateTime timestamp, String source) {
        this.level = level;
        this.message = message;
        this.timestamp = timestamp;
        this.source = source;
    }

    public String getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getSource() {
        return source;
    }
}
