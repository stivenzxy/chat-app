package com.clientApplication.events;

public record PrivateMessageEvent(String sender, String content, boolean isEcho) {
    public PrivateMessageEvent(String sender, String content) {
        this(sender, content, false);
    }
}