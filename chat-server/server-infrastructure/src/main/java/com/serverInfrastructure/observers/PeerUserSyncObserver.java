package com.serverInfrastructure.observers;

import com.serverApplication.ports.peer.PeerUserSyncControl;
import com.serverDomain.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerUserSyncObserver implements ActiveUserObserver {

    private static final Logger logger = LoggerFactory.getLogger(PeerUserSyncObserver.class);

    private final PeerUserSyncControl peerUserSyncControl;

    public PeerUserSyncObserver(PeerUserSyncControl peerUserSyncControl) {
        this.peerUserSyncControl = peerUserSyncControl;
    }

    @Override
    public void onUserLoggedIn(User user) {
        String username = user.getUsername().value();
        logger.info("Usuario {} conectado localmente - notificando a peers P2P", username);

        try {
            peerUserSyncControl.notifyUserChangeToPeers(username, "USER_JOINED");
        } catch (Exception e) {
            logger.error("Error notificando conexión de usuario {} a peers P2P: {}", username, e.getMessage());
        }
    }

    @Override
    public void onUserLoggedOut(User user, boolean isLastSession) {
        if (isLastSession) {
            String username = user.getUsername().value();
            logger.info("Usuario {} desconectado (última sesión) - notificando a peers P2P", username);

            try {
                peerUserSyncControl.notifyUserChangeToPeers(username, "USER_LEFT");
            } catch (Exception e) {
                logger.error("Error notificando desconexión de usuario {} a peers P2P: {}", username, e.getMessage());
            }
        }
    }
}