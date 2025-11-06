package com.serverApplication.dto;

import java.util.List;

public record UserSyncInfo(
    String serverId,
    List<String> connectedUsers,
    String action
) {

    public static UserSyncInfo createFullSync(String serverId, List<String> users) {
        return new UserSyncInfo(serverId, users, "SYNC_ALL");
    }

    public static UserSyncInfo createUserJoined(String serverId, String username) {
        return new UserSyncInfo(serverId, List.of(username), "USER_JOINED");
    }

    public static UserSyncInfo createUserLeft(String serverId, String username) {
        return new UserSyncInfo(serverId, List.of(username), "USER_LEFT");
    }

    public String toProtocol() {
        String usersStr = String.join(",", connectedUsers);
        return String.format("P2P_USER_SYNC|serverId=%s|action=%s|users=%s", 
            serverId, action, usersStr);
    }

    public static UserSyncInfo fromProtocol(String protocolMessage) {
        try {
            if (!protocolMessage.startsWith("P2P_USER_SYNC|")) {
                throw new IllegalArgumentException("Mensaje no es P2P_USER_SYNC");
            }
            
            String serverId = null;
            String action = null;
            List<String> users = List.of();
            
            String[] parts = protocolMessage.split("\\|");
            for (String part : parts) {
                if (part.startsWith("serverId=")) {
                    serverId = part.substring(9);
                } else if (part.startsWith("action=")) {
                    action = part.substring(7);
                } else if (part.startsWith("users=")) {
                    String usersStr = part.substring(6);
                    if (!usersStr.isEmpty()) {
                        users = List.of(usersStr.split(","));
                    }
                }
            }
            
            return new UserSyncInfo(serverId, users, action);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando P2P_USER_SYNC: " + e.getMessage());
        }
    }
}
