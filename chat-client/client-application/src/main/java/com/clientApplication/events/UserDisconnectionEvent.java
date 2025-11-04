package com.clientApplication.events;

public record UserDisconnectionEvent(String username) {
    public String userId() {
        return username;
    }
}