package com.serverInfrastructure.factories;

import com.serverDomain.repositories.PeerRegistryRepository;
import com.serverDomain.repositories.UserRepository;
import com.serverInfrastructure.adapters.peer.managers.*;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import com.serverInfrastructure.persistence.repositories.PeerRegistryRepositoryImpl;
import com.serverInfrastructure.persistence.repository.UserManagementRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

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
    
    public static PeerUserReplicationManager createUserReplicationManager() {
        return createUserReplicationManager(null);
    }
    
    public static PeerUserReplicationManager createUserReplicationManager(Consumer<Void> userReplicationCallback) {
        logger.debug("Creando PeerUserReplicationManager" + 
                    (userReplicationCallback != null ? " con callback de notificación" : ""));
        UserRepository userRepository = new UserManagementRepository();
        PeerUserReplicationManager manager = new PeerUserReplicationManager(userRepository);
        if (userReplicationCallback != null) {
            manager.setOnUserReplicationCallback(userReplicationCallback);
        }
        return manager;
    }
    
    public static PeerPeerReplicationManager createPeerReplicationManager() {
        logger.debug("Creando PeerPeerReplicationManager");
        ConnectionManager connManager = ConnectionManager.getInstance();
        PeerRegistryRepository peerRepository = new PeerRegistryRepositoryImpl(connManager);
        return new PeerPeerReplicationManager(peerRepository);
    }
}
