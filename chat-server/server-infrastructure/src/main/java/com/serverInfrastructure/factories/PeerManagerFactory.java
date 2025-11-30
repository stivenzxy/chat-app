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

    public static PeerEntityReplicationManager createEntityReplicationManager() {
        logger.debug("Creando PeerEntityReplicationManager");
        return new PeerEntityReplicationManager();
    }

    public static PeerFullSyncManager createPeerFullSyncManager(Consumer<String> broadcaster) {
        return createPeerFullSyncManager(broadcaster, null);
    }
    
    public static PeerFullSyncManager createPeerFullSyncManager(Consumer<String> broadcaster, Consumer<Void> syncCompleteCallback) {
        logger.debug("Creando PeerFullSyncManager" +
                (syncCompleteCallback != null ? " con callback de notificación" : ""));
        com.serverInfrastructure.persistence.repository.UserManagementRepository userRepository = new com.serverInfrastructure.persistence.repository.UserManagementRepository();
        com.serverInfrastructure.persistence.repository.ChannelRepositoryImpl channelRepository = new com.serverInfrastructure.persistence.repository.ChannelRepositoryImpl();
        com.serverInfrastructure.persistence.repository.ChannelInviteRepositoryImpl inviteRepository = new com.serverInfrastructure.persistence.repository.ChannelInviteRepositoryImpl();

        com.serverInfrastructure.services.sync.DatabaseSynchronizationService dbSyncService = new com.serverInfrastructure.services.sync.DatabaseSynchronizationService(
                userRepository, channelRepository, inviteRepository);

        com.serverInfrastructure.services.sync.PresenceReplicationService presenceService = new com.serverInfrastructure.services.sync.PresenceReplicationService(
                broadcaster);

        PeerFullSyncManager manager = new PeerFullSyncManager(dbSyncService, presenceService);
        if (syncCompleteCallback != null) {
            manager.setOnSyncCompleteCallback(syncCompleteCallback);
        }
        return manager;
    }
}
