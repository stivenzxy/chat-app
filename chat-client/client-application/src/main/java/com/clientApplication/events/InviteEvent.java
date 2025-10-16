package com.clientApplication.events;

public record InviteEvent(int inviteId, int channelId, String channelName, String visibility, String inviterUsername) {}


