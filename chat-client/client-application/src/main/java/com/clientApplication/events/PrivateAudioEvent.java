package com.clientApplication.events;

public record PrivateAudioEvent(String sender, String audioBase64, boolean isEcho) {
    public PrivateAudioEvent(String sender, String audioBase64) {
        this(sender, audioBase64, false);
    }
}