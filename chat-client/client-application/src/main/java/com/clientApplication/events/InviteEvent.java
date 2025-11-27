package com.clientApplication.events;

public record InviteEvent(String inviteId, String channelId, String channelName, String visibility,
        String inviterUsername) {
}
