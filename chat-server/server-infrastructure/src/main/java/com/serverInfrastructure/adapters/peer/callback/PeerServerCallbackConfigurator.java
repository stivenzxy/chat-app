package com.serverInfrastructure.adapters.peer.callback;

import com.serverInfrastructure.adapters.peer.managers.PeerEntityReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerMessageRoutingManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserSyncManager;
import com.serverInfrastructure.adapters.peer.connection.IncomingConnectionHandler;
import com.serverInfrastructure.adapters.peer.managers.PeerPeerReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserReplicationManager;
import com.serverInfrastructure.network.peerTcp.PeerTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configures all callbacks for the PeerTcpServer.
 * Centralizes the wiring of server events to appropriate handlers.
 */
public class PeerServerCallbackConfigurator {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerServerCallbackConfigurator.class);
    
    private final IncomingConnectionHandler incomingHandler;
    private final PeerUserSyncManager userSyncManager;
    private final PeerMessageRoutingManager messageRoutingManager;
    private final PeerUserReplicationManager userReplicationManager;
    private final PeerPeerReplicationManager peerReplicationManager;
    private final PeerEntityReplicationManager entityReplicationManager;
    
    public PeerServerCallbackConfigurator(
            IncomingConnectionHandler incomingHandler,
            PeerUserSyncManager userSyncManager,
            PeerMessageRoutingManager messageRoutingManager,
            PeerUserReplicationManager userReplicationManager,
            PeerPeerReplicationManager peerReplicationManager,
            PeerEntityReplicationManager entityReplicationManager) {
        
        this.incomingHandler = incomingHandler;
        this.userSyncManager = userSyncManager;
        this.messageRoutingManager = messageRoutingManager;
        this.userReplicationManager = userReplicationManager;
        this.peerReplicationManager = peerReplicationManager;
        this.entityReplicationManager = entityReplicationManager;
    }
    
    /**
     * Configures all callbacks for the given PeerTcpServer.
     * 
     * @param peerServer the server to configure
     * @param peerPort the port on which the server is listening
     */
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
    
    /**
     * Configures the user synchronization callback.
     * 
     * @param peerServer the server to configure
     */
    private void configureUserSyncCallback(PeerTcpServer peerServer) {
        peerServer.setOnUserSyncReceived((peerId, message) -> {
            logger.debug("Procesando sincronización de usuarios de peer entrante {}", peerId);
            userSyncManager.handleUserSyncMessage(peerId, message);
        });
    }
    
    /**
     * Configures the peer connection callback for incoming connections.
     * 
     * @param peerServer the server to configure
     */
    private void configurePeerConnectionCallback(PeerTcpServer peerServer) {
        peerServer.setOnPeerConnected(incomingHandler::registerIncomingPeer);
    }
    
    /**
     * Configures the callback to provide local user sync data.
     * 
     * @param peerServer the server to configure
     * @param peerPort the port for identity inclusion
     */
    private void configureLocalUserSyncCallback(PeerTcpServer peerServer, int peerPort) {
        peerServer.setOnGetLocalUserSync(() -> 
            userSyncManager.generateInitialSyncMessage(peerPort)
        );
    }
    
    /**
     * Configures the callback for routed private messages.
     * 
     * @param peerServer the server to configure
     */
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
            }
        });
    }
    
    /**
     * Configures the callback to send replication data to incoming peers.
     * 
     * @param peerServer the server to configure
     */
    private void configureIncomingPeerReplicationCallback(PeerTcpServer peerServer) {
        peerServer.setOnIncomingPeerConnected(peerId -> {
            logger.info("Enviando replicación a peer entrante {}", peerId);
            
            // Send users replication
            try {
                userReplicationManager.sendUsersToPeer(peerId);
                logger.debug("Usuarios enviados a peer entrante {}", peerId);
            } catch (Exception e) {
                logger.error("Error enviando usuarios a peer entrante {}: {}", peerId, e.getMessage());
            }
            
            // Send peers replication  
            try {
                peerReplicationManager.sendPeersToPeer(peerId);
                logger.debug("Peers enviados a peer entrante {}", peerId);
            } catch (Exception e) {
                logger.error("Error enviando peers a peer entrante {}: {}", peerId, e.getMessage());
            }
        });
    }
}
