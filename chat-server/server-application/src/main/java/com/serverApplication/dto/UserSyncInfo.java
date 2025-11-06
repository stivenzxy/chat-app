package com.serverApplication.dto;

import java.util.List;

/**
 * DTO para sincronizar información de usuarios entre servidores P2P.
 * 
 * Permite que los servidores compartan qué usuarios están conectados en cada servidor.
 */
public record UserSyncInfo(
    String serverId,           // "192.168.1.10:12346" - ID del servidor que envía
    List<String> connectedUsers, // ["user1", "user2", "user3"]
    String action              // "SYNC_ALL", "USER_JOINED", "USER_LEFT"
) {
    
    /**
     * Crea mensaje de sincronización completa (al conectarse servers).
     */
    public static UserSyncInfo createFullSync(String serverId, List<String> users) {
        return new UserSyncInfo(serverId, users, "SYNC_ALL");
    }
    
    /**
     * Crea notificación de usuario que se conectó.
     */
    public static UserSyncInfo createUserJoined(String serverId, String username) {
        return new UserSyncInfo(serverId, List.of(username), "USER_JOINED");
    }
    
    /**
     * Crea notificación de usuario que se desconectó.
     */
    public static UserSyncInfo createUserLeft(String serverId, String username) {
        return new UserSyncInfo(serverId, List.of(username), "USER_LEFT");
    }
    
    /**
     * Serializa a formato de protocolo P2P.
     * Formato: "P2P_USER_SYNC|serverId=x|action=y|users=user1,user2,user3"
     */
    public String toProtocol() {
        String usersStr = String.join(",", connectedUsers);
        return String.format("P2P_USER_SYNC|serverId=%s|action=%s|users=%s", 
            serverId, action, usersStr);
    }
    
    /**
     * Deserializa desde formato de protocolo P2P.
     */
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
