package com.serverInfrastructure.adapters.peer.callback;

import com.serverInfrastructure.adapters.peer.managers.PeerEntityReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerFullSyncManager;
import com.serverInfrastructure.adapters.peer.managers.PeerMessageRoutingManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserSyncManager;
import com.serverInfrastructure.adapters.peer.connection.IncomingConnectionHandler;
import com.serverInfrastructure.adapters.peer.managers.PeerPeerReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserReplicationManager;
import com.serverInfrastructure.network.peerTcp.PeerTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerServerCallbackConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(PeerServerCallbackConfigurator.class);

    private final IncomingConnectionHandler incomingHandler;
    private final PeerUserSyncManager userSyncManager;
    private final PeerMessageRoutingManager messageRoutingManager;
    private final PeerUserReplicationManager userReplicationManager;
    private final PeerPeerReplicationManager peerReplicationManager;
    private final PeerEntityReplicationManager entityReplicationManager;
    private final PeerFullSyncManager fullSyncManager;

    public PeerServerCallbackConfigurator(
            IncomingConnectionHandler incomingHandler,
            PeerUserSyncManager userSyncManager,
            PeerMessageRoutingManager messageRoutingManager,
            PeerUserReplicationManager userReplicationManager,
            PeerPeerReplicationManager peerReplicationManager,
            PeerEntityReplicationManager entityReplicationManager,
            PeerFullSyncManager fullSyncManager) {

        this.incomingHandler = incomingHandler;
        this.userSyncManager = userSyncManager;
        this.messageRoutingManager = messageRoutingManager;
        this.userReplicationManager = userReplicationManager;
        this.peerReplicationManager = peerReplicationManager;
        this.entityReplicationManager = entityReplicationManager;
        this.fullSyncManager = fullSyncManager;
    }

    public void configureServerCallbacks(PeerTcpServer peerServer, int peerPort) {
        if (peerServer == null) {
            logger.warn("PeerTcpServer es nulo, no se pueden configurar callbacks");
            return;
        }

        configureUserSyncCallback(peerServer);
        configurePeerConnectionCallback(peerServer);
        configureLocalUserSyncCallback(peerServer, peerPort);
        configurePrivateMessageCallback(peerServer);
        configureIncomingPeerReplicationCallback(peerServer);

        logger.info("Callbacks del servidor P2P configurados exitosamente");
    }

    private void configureUserSyncCallback(PeerTcpServer peerServer) {
        peerServer.setOnUserSyncReceived((peerId, message) -> {
            logger.debug("Procesando sincronización de usuarios de peer entrante {}", peerId);
            userSyncManager.handleUserSyncMessage(peerId, message);
        });
    }

    private void configurePeerConnectionCallback(PeerTcpServer peerServer) {
        peerServer.setOnPeerConnected(incomingHandler::registerIncomingPeer);
    }

    private void configureLocalUserSyncCallback(PeerTcpServer peerServer, int peerPort) {
        peerServer.setOnGetLocalUserSync(() -> userSyncManager.generateInitialSyncMessage(peerPort));
        
        peerServer.setOnGetConnectedUsersSync(() -> userSyncManager.generateConnectedUsersSyncMessage(peerPort));
    }

    private void configurePrivateMessageCallback(PeerTcpServer peerServer) {
        peerServer.setOnPrivateMessageReceived((peerId, message) -> {
            if (message.startsWith("P2P_BATCH_USER_REPLICATION")) {
                logger.info("Recibiendo replicación de usuarios desde peer entrante {}", peerId);
                userReplicationManager.handleIncomingUserReplication(peerId, message);
            } else if (message.startsWith("P2P_BATCH_PEER_DISCOVERY")) {
                logger.info("Recibiendo descubrimiento de peers desde peer entrante {}", peerId);
                peerReplicationManager.handleIncomingPeerReplication(peerId, message);
            } else if (message.startsWith("P2P_REPLICATE_")) {
                entityReplicationManager.handleIncomingReplication(peerId, message);
            } else if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                messageRoutingManager.handlePrivateAudioRouted(peerId, message);
            } else if (message.startsWith("P2P_ROUTE_PRIVATE")) {
                messageRoutingManager.handlePrivateMessageRouted(peerId, message);
            } else if (message.startsWith("P2P_CHANNEL_INVITE")) {
                messageRoutingManager.handleChannelInviteRouted(peerId, message);
            } else if (message.startsWith("P2P_CHANNEL_MESSAGE")) {
                messageRoutingManager.handleChannelMessageRouted(peerId, message);
            } else if (message.startsWith("P2P_FULL_SYNC_REQUEST")) {
                fullSyncManager.handleFullSyncRequest(peerId);
            } else if (message.startsWith("P2P_FULL_SYNC_RESPONSE")) {
                fullSyncManager.handleFullSyncResponse(peerId, message);
            } else if (message.startsWith("P2P_USER_STATUS_UPDATE")) {
                fullSyncManager.handleUserStatusUpdate(peerId, message);
            }
        });
    }

    private void configureIncomingPeerReplicationCallback(PeerTcpServer peerServer) {
        peerServer.setOnIncomingPeerConnected(peerId -> {
            logger.info("Enviando replicación a peer entrante {}", peerId);

            try {
                userReplicationManager.sendUsersToPeer(peerId);
                logger.debug("Usuarios enviados a peer entrante {}", peerId);
            } catch (Exception e) {
                logger.error("Error enviando usuarios a peer entrante {}: {}", peerId, e.getMessage());
            }

            try {
                peerReplicationManager.sendPeersToPeer(peerId);
                logger.debug("Peers enviados a peer entrante {}", peerId);
            } catch (Exception e) {
                logger.error("Error enviando peers a peer entrante {}: {}", peerId, e.getMessage());
            }

            try {
                logger.info("Enviando nuestra BD completa al peer entrante {}", peerId);
                fullSyncManager.sendFullDatabaseToPeer(peerId);
            } catch (Exception e) {
                logger.error("Error enviando BD completa a peer entrante {}: {}", peerId, e.getMessage());
            }

            try {
                logger.info("Solicitando sincronización completa de BD desde peer entrante {}", peerId);
                fullSyncManager.requestFullSync(peerId);
            } catch (Exception e) {
                logger.error("Error solicitando full sync a peer entrante {}: {}", peerId, e.getMessage());
            }
        });
    }
}
