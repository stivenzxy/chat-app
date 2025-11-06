package com.serverApplication.ports.peer;

@FunctionalInterface
public interface ClientMessageBroadcaster {
    void broadcast(String targetUsername, String message, String excludeConnectionId);
}
