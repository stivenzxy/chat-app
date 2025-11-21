package com.serverInfrastructure.factories;

import com.serverDomain.repositories.PeerRegistryRepository;
import com.serverDomain.repositories.UserRepository;
import com.serverInfrastructure.adapters.peer.Managers.*;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import com.serverInfrastructure.persistence.repositories.PeerRegistryRepositoryImpl;
import com.serverInfrastructure.persistence.repository.UserManagementRepository;
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
    
    public static PeerUserReplicationManager createUserReplicationManager() {
        logger.debug("Creando PeerUserReplicationManager");
        UserRepository userRepository = new UserManagementRepository();
        return new PeerUserReplicationManager(userRepository);
    }
    
    public static PeerPeerReplicationManager createPeerReplicationManager() {
        logger.debug("Creando PeerPeerReplicationManager");
        ConnectionManager connManager = ConnectionManager.getInstance();
        PeerRegistryRepository peerRepository = new PeerRegistryRepositoryImpl(connManager);
        return new PeerPeerReplicationManager(peerRepository);
    }
}
