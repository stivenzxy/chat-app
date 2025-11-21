package com.serverInfrastructure.adapters.peer.connection;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverInfrastructure.adapters.peer.managers.PeerConnectionManager;
import com.serverInfrastructure.adapters.peer.managers.PeerObserverNotifier;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingConnectionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(IncomingConnectionHandler.class);
    
    private final PeerConnectionValidator validator;
    private final PeerConnectionManager connectionManager;
    private final PeerObserverNotifier observerNotifier;
    
    public IncomingConnectionHandler(
            PeerConnectionValidator validator,
            PeerConnectionManager connectionManager,
            PeerObserverNotifier observerNotifier) {
        
        this.validator = validator;
        this.connectionManager = connectionManager;
        this.observerNotifier = observerNotifier;
    }
    
    public void registerIncomingPeer(ConnectedPeerInfo peerInfo) {
        if (peerInfo == null) {
            logger.debug("Received null peerInfo, rejecting connection");
            return;
        }
        
        String peerId = peerInfo.peerId();
        int remotePort = PeerIdParser.extractPort(peerId);
        
        if (remotePort == -1) {
            logger.debug("No se pudo parsear puerto de peerId {}, rechazando", peerId);
            return;
        }
        
        if (validator.isEphemeralPort(remotePort)) {
            logger.debug("Ignorando conexión entrante desde puerto efímero {} (esperado no-efímero)", remotePort);
            return;
        }
        
        logger.info("Aceptando conexión entrante de peer {} (puerto válido: {})", peerId, remotePort);
        
        try {
            connectionManager.registerIncomingPeer(peerInfo);
            observerNotifier.notifyPeerConnected(peerInfo);
            logger.info("Peer entrante registrado: {}", peerId);
        } catch (Exception e) {
            logger.warn("Error registrando peer entrante: {}", e.getMessage());
        }
    }
}