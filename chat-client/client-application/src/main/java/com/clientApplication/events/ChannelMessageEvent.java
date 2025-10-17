package com.clientApplication.events;

public class ChannelMessageEvent {
    private final int channelId;
    private final String sender;
    private final String content;
    public ChannelMessageEvent(int channelId, String sender, String content) {
        this.channelId = channelId; this.sender = sender; this.content = content;
    }
    public int getChannelId() { return channelId; }
    public String getSender() { return sender; }
    public String getContent() { return content; }
}


