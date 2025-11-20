package com.serverInfrastructure.factories;

import com.serverInfrastructure.adapters.peer.Managers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerManagerFactory {
    private static final Logger logger = LoggerFactory.getLogger(PeerManagerFactory.class);

    public static PeerConnectionManager createConnectionManager() {
        logger.debug("Creando PeerConnectionManager");
        return new PeerConnectionManager();
    }

    public static PeerUserSyncManager createUserSyncManager() {
        logger.debug("Creando PeerUserSyncManager");
        return new PeerUserSyncManager();
    }

    public static PeerMessageRoutingManager createMessageRoutingManager() {
        logger.debug("Creando PeerMessageRoutingManager");
        return new PeerMessageRoutingManager();
    }

    public static PeerObserverNotifier createObserverNotifier() {
        logger.debug("Creando PeerObserverNotifier");
        return new PeerObserverNotifier();
    }
}
