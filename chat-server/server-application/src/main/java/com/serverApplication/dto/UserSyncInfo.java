package com.serverApplication.dto;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record UserSyncInfo(
    String serverId,
    List<String> connectedUsers,
    Map<String, String> userPhotos,
    String action
) {

    public static UserSyncInfo createFullSync(String serverId, List<String> users, Map<String, String> photos) {
        return new UserSyncInfo(serverId, users, photos, "SYNC_ALL");
    }

    public static UserSyncInfo createUserJoined(String serverId, String username, String photoBase64) {
        Map<String, String> photos = new HashMap<>();
        photos.put(username, photoBase64);
        return new UserSyncInfo(serverId, List.of(username), photos, "USER_JOINED");
    }

    public static UserSyncInfo createUserLeft(String serverId, String username) {
        return new UserSyncInfo(serverId, List.of(username), new HashMap<>(), "USER_LEFT");
    }

    public String toProtocol() {
        String usersStr = String.join(",", connectedUsers);
        
        String photosStr = userPhotos.entrySet().stream()
            .map(entry -> entry.getKey() + "~" + (entry.getValue() != null ? entry.getValue() : ""))
            .collect(Collectors.joining(";"));
        
        return String.format("P2P_USER_SYNC|serverId=%s|action=%s|users=%s|photos=%s", 
            serverId, action, usersStr, photosStr);
    }

    public static UserSyncInfo fromProtocol(String protocolMessage) {
        try {
            if (!protocolMessage.startsWith("P2P_USER_SYNC|")) {
                throw new IllegalArgumentException("Mensaje no es P2P_USER_SYNC");
            }
            
            String serverId = null;
            String action = null;
            List<String> users = List.of();
            Map<String, String> photos = new HashMap<>();
            
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
                } else if (part.startsWith("photos=")) {
                    String photosStr = part.substring(7);
                    if (!photosStr.isEmpty()) {
                        String[] photoEntries = photosStr.split(";");
                        for (String entry : photoEntries) {
                            String[] usernameAndPhoto = entry.split("~", 2);
                            if (usernameAndPhoto.length == 2) {
                                String username = usernameAndPhoto[0];
                                String photoBase64 = usernameAndPhoto[1].isEmpty() ? "" : usernameAndPhoto[1];
                                photos.put(username, photoBase64);
                            } else if (usernameAndPhoto.length == 1 && !usernameAndPhoto[0].isEmpty()) {
                                photos.put(usernameAndPhoto[0], "");
                            }
                        }
                    }
                }
            }
            
            return new UserSyncInfo(serverId, users, photos, action);
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Error parseando P2P_USER_SYNC: " + e.getMessage());
        }
    }
}
