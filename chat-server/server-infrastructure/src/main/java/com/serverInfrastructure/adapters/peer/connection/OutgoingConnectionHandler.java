package com.serverInfrastructure.adapters.peer.connection;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverInfrastructure.adapters.peer.managers.PeerConnectionManager;
import com.serverInfrastructure.adapters.peer.managers.PeerMessageRoutingManager;
import com.serverInfrastructure.adapters.peer.managers.PeerObserverNotifier;
import com.serverInfrastructure.adapters.peer.managers.PeerUserSyncManager;
import com.serverInfrastructure.adapters.peer.discovery.PeerDiscoveryHandler;
import com.serverInfrastructure.adapters.peer.managers.PeerPeerReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserReplicationManager;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutgoingConnectionHandler {

    private static final Logger logger = LoggerFactory.getLogger(OutgoingConnectionHandler.class);

    private final PeerConnectionValidator validator;
    private final PeerConnectionManager connectionManager;
    private final PeerUserSyncManager userSyncManager;
    private final PeerObserverNotifier observerNotifier;
    private final PeerDiscoveryHandler discoveryHandler;
    private final PeerMessageRoutingManager messageRoutingManager;
    private final PeerUserReplicationManager userReplicationManager;
    private final PeerPeerReplicationManager peerReplicationManager;

    private int localServerPort = -1;

    public OutgoingConnectionHandler(
            PeerConnectionValidator validator,
            PeerConnectionManager connectionManager,
            PeerUserSyncManager userSyncManager,
            PeerObserverNotifier observerNotifier,
            PeerDiscoveryHandler discoveryHandler,
            PeerMessageRoutingManager messageRoutingManager,
            PeerUserReplicationManager userReplicationManager,
            PeerPeerReplicationManager peerReplicationManager) {

        this.validator = validator;
        this.connectionManager = connectionManager;
        this.userSyncManager = userSyncManager;
        this.observerNotifier = observerNotifier;
        this.discoveryHandler = discoveryHandler;
        this.messageRoutingManager = messageRoutingManager;
        this.userReplicationManager = userReplicationManager;
        this.peerReplicationManager = peerReplicationManager;
    }

    public void setLocalServerPort(int port) {
        this.localServerPort = port;
    }

    public boolean connectToPeer(String ip, int port) {
        String peerId = PeerIdParser.create(ip, port);

        if (localServerPort != -1 && validator.isLocalAddress(ip, port, localServerPort)) {
            logger.info("Auto-conexión detectada y prevenida para {}:{}", ip, port);
            return false;
        }

        return connectionManager.connectToPeer(
            ip,
            port,
            // onConnectionRejected
            () -> {
                ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(peerId);
                if (peerInfo != null) {
                    observerNotifier.notifyPeerDisconnected(peerInfo.withStatus("Conexión Rechazada"));
                }
                observerNotifier.notifyPeerConnectionError(peerId, 
                    "Conexión rechazada - intento de conectar P2P al puerto de clientes");
            },
            // onUserSyncReceived
            message -> {
                logger.debug("Listado de usuarios actualizado por peer {}", peerId);
                userSyncManager.handleUserSyncMessage(peerId, message);
            },
            // onPeerListReceived
            peerListMessage -> discoveryHandler.processPeerList(peerId, peerListMessage),

            // onPrivateMessageReceived
            (sourcePeerId, message) -> {
                if (message.startsWith("P2P_BATCH_USER_REPLICATION")) {
                    logger.info("Recibiendo replicación de usuarios desde peer {}", sourcePeerId);
                    userReplicationManager.handleIncomingUserReplication(sourcePeerId, message);
                } else if (message.startsWith("P2P_BATCH_PEER_DISCOVERY")) {
                    logger.info("Recibiendo descubrimiento de peers desde {}", sourcePeerId);
                    peerReplicationManager.handleIncomingPeerReplication(sourcePeerId, message);
                } else if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                    messageRoutingManager.handlePrivateAudioRouted(sourcePeerId, message);
                } else if (message.startsWith("P2P_ROUTE_PRIVATE")) {
                    messageRoutingManager.handlePrivateMessageRouted(sourcePeerId, message);
                } else if (message.startsWith("P2P_CHANNEL_INVITE")) {
                    messageRoutingManager.handleChannelInviteRouted(sourcePeerId, message);
                } else if (message.startsWith("P2P_CHANNEL_MESSAGE")) {
                    messageRoutingManager.handleChannelMessageRouted(sourcePeerId, message);
                }
            },
            // onConnectionSuccess
            confirmedPeerId -> {
                ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(confirmedPeerId);
                if (peerInfo != null) {
                    observerNotifier.notifyPeerConnected(peerInfo);
                }
                userSyncManager.sendFullUserSyncToPeer(confirmedPeerId,
                    msg -> connectionManager.sendMessageToPeer(confirmedPeerId, msg));
                    
                // Send user replication after connection
                logger.info("Enviando usuarios para replicación al peer {}", confirmedPeerId);
                userReplicationManager.sendUsersToPeer(confirmedPeerId);
                
                // Send known peers for transitive discovery
                logger.info("Enviando peers conocidos para descubrimiento transitivo a {}", confirmedPeerId);
                peerReplicationManager.sendPeersToPeer(confirmedPeerId);
            }
        );
    }
}
