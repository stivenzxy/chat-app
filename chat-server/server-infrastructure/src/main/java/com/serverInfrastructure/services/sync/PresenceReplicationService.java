package com.serverInfrastructure.services.sync;

import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class PresenceReplicationService {
    private static final Logger logger = LoggerFactory.getLogger(PresenceReplicationService.class);

    private Consumer<String> broadcaster;

    public PresenceReplicationService(Consumer<String> broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void setBroadcaster(Consumer<String> broadcaster) {
        this.broadcaster = broadcaster;
    }

    public void broadcastUserStatus(String username, boolean isOnline) {
        if (broadcaster != null) {
            try {
                String message = "P2P_USER_STATUS_UPDATE|" + username + "|" + isOnline;
                broadcaster.accept(message);
                logger.debug("Broadcasted status for user {}: {}", username, isOnline ? "ONLINE" : "OFFLINE");
            } catch (Exception e) {
                logger.error("Error broadcasting user status: {}", e.getMessage());
            }
        }
    }

    public void handleUserStatusUpdate(String username, boolean isOnline) {
        logger.info("Recibida actualización de estado remoto: {} -> {}", username, isOnline ? "ONLINE" : "OFFLINE");
    }
}
