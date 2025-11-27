package com.clientApplication.events;

public class ChannelAudioEvent {
    private final String channelId;
    private final String sender;
    private final String audioBase64;

    public ChannelAudioEvent(String channelId, String sender, String audioBase64) {
        this.channelId = channelId;
        this.sender = sender;
        this.audioBase64 = audioBase64;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getSender() {
        return sender;
    }

    public String getAudioBase64() {
        return audioBase64;
    }
}
