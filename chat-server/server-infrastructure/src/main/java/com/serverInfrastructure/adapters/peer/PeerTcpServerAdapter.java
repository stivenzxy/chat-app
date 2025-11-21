package com.serverInfrastructure.adapters.peer;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverApplication.ports.PeerConnectionObserver;
import com.serverApplication.ports.peer.ClientMessageBroadcaster;
import com.serverApplication.ports.peer.PeerNetworkControl;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverInfrastructure.adapters.peer.managers.PeerConnectionManager;
import com.serverInfrastructure.adapters.peer.managers.PeerMessageRoutingManager;
import com.serverInfrastructure.adapters.peer.managers.PeerObserverNotifier;
import com.serverInfrastructure.adapters.peer.managers.PeerUserSyncManager;
import com.serverInfrastructure.adapters.peer.callback.PeerServerCallbackConfigurator;
import com.serverInfrastructure.adapters.peer.connection.IncomingConnectionHandler;
import com.serverInfrastructure.adapters.peer.connection.OutgoingConnectionHandler;
import com.serverInfrastructure.adapters.peer.discovery.PeerAutoReconnectService;
import com.serverInfrastructure.adapters.peer.discovery.PeerDiscoveryHandler;
import com.serverInfrastructure.adapters.peer.lifecycle.LocalServerIdentityProvider;
import com.serverInfrastructure.adapters.peer.lifecycle.ServerLifecycleManager;
import com.serverInfrastructure.adapters.peer.managers.PeerPeerReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserReplicationManager;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PeerTcpServerAdapter implements PeerNetworkControl {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerTcpServerAdapter.class);

    private String localServerId;

    private final ServerLifecycleManager lifecycleManager;
    private final LocalServerIdentityProvider identityProvider;

    private final IncomingConnectionHandler incomingHandler;
    private final OutgoingConnectionHandler outgoingHandler;

    private final PeerDiscoveryHandler discoveryHandler;
    private final PeerAutoReconnectService autoReconnectService;

    private final PeerServerCallbackConfigurator callbackConfigurator;

    private final PeerConnectionManager connectionManager;
    private final PeerUserSyncManager userSyncManager;
    private final PeerMessageRoutingManager messageRoutingManager;
    private final PeerObserverNotifier observerNotifier;
    private final PeerUserReplicationManager userReplicationManager;
    private final PeerPeerReplicationManager peerReplicationManager;

    public PeerTcpServerAdapter(ServerLifecycleManager lifecycleManager, LocalServerIdentityProvider identityProvider,
                                IncomingConnectionHandler incomingHandler, OutgoingConnectionHandler outgoingHandler,
                                PeerDiscoveryHandler discoveryHandler, PeerAutoReconnectService autoReconnectService,
                                PeerServerCallbackConfigurator callbackConfigurator, PeerConnectionManager connectionManager,
                                PeerUserSyncManager userSyncManager, PeerMessageRoutingManager messageRoutingManager,
                                PeerObserverNotifier observerNotifier, PeerUserReplicationManager userReplicationManager,
                                PeerPeerReplicationManager peerReplicationManager) {
        
        this.lifecycleManager = lifecycleManager;
        this.identityProvider = identityProvider;
        this.incomingHandler = incomingHandler;
        this.outgoingHandler = outgoingHandler;
        this.discoveryHandler = discoveryHandler;
        this.autoReconnectService = autoReconnectService;
        this.callbackConfigurator = callbackConfigurator;
        this.connectionManager = connectionManager;
        this.userSyncManager = userSyncManager;
        this.messageRoutingManager = messageRoutingManager;
        this.observerNotifier = observerNotifier;
        this.userReplicationManager = userReplicationManager;
        this.peerReplicationManager = peerReplicationManager;

        configureManagerDependencies();
    }

    public void setInviteRepository(ChannelInviteRepository repository) {
        messageRoutingManager.setInviteRepository(repository);
        logger.info("ChannelInviteRepository inyectado en PeerTcpServerAdapter");
    }

    private void configureManagerDependencies() {
        userSyncManager.setPeerBroadcastCallback(connectionManager::broadcastToPeers);
        messageRoutingManager.setRemoteUsersProvider(userSyncManager::getAllUsersAcrossPeers);

        messageRoutingManager.setPeerMessageSender((peerId, message) -> {
            logger.debug("Intentando enviar mensaje P2P a {}: {}", peerId, message.substring(0, Math.min(50, message.length())));
            boolean sentViaOutgoing = connectionManager.sendMessageToPeer(peerId, message);
            if (!sentViaOutgoing) {
                logger.debug("No se pudo enviar via conexión saliente, intentando via conexión entrante");
                if (lifecycleManager.isRunning()) {
                    boolean sentViaIncoming = lifecycleManager.getServer().sendMessageToIncomingPeer(peerId, message);
                    if (sentViaIncoming) {
                        logger.debug("Mensaje enviado exitosamente via conexión entrante a {}", peerId);
                    } else {
                        logger.warn("No se pudo enviar mensaje P2P a {} (ni saliente ni entrante)", peerId);
                    }
                } else {
                    logger.warn("PeerServer no está corriendo, no se puede enviar via conexión entrante");
                }
            } else {
                logger.debug("Mensaje enviado exitosamente via conexión saliente a {}", peerId);
            }
        });
        
        // Configure user replication manager - usar misma lógica que messageRoutingManager
        userReplicationManager.setPeerMessageSender((peerId, message) -> {
            logger.debug("Enviando replicación de usuarios a {}", peerId);
            boolean sentViaOutgoing = connectionManager.sendMessageToPeer(peerId, message);
            if (!sentViaOutgoing) {
                logger.debug("No se pudo enviar via saliente, intentando via entrante para {}", peerId);
                if (lifecycleManager.isRunning()) {
                    boolean sentViaIncoming = lifecycleManager.getServer().sendMessageToIncomingPeer(peerId, message);
                    if (sentViaIncoming) {
                        logger.debug("Replicación de usuarios enviada via conexión entrante a {}", peerId);
                    } else {
                        logger.warn("No se pudo enviar replicación de usuarios a {} (ni saliente ni entrante)", peerId);
                    }
                }
            } else {
                logger.debug("Replicación de usuarios enviada exitosamente a {}", peerId);
            }
        });
        
        // Configure peer replication manager  
        peerReplicationManager.setPeerMessageSender(connectionManager::sendMessageToPeer);
        peerReplicationManager.setAutoConnectCallback(this::connectToPeerById);
    }
    
    /**
     * Helper method to connect to a peer using only its peerId.
     * Extracts IP and port from peerId and delegates to connectToPeer(ip, port).
     */
    private void connectToPeerById(String peerId) {
        try {
            String ip = PeerIdParser.extractIp(peerId);
            int port = PeerIdParser.extractPort(peerId);
            
            if (ip != null && port != -1) {
                logger.debug("Auto-conectando a peer descubierto transitivamente: {}:{}", ip, port);
                connectToPeer(ip, port);
            } else {
                logger.warn("No se pudo extraer IP/puerto de peerId: {}", peerId);
            }
        } catch (Exception e) {
            logger.error("Error auto-conectando a peer {}: {}", peerId, e.getMessage());
        }
    }


    @Override
    public boolean startPeerServer() {
        if (lifecycleManager.isRunning()) {
            logger.warn("Servidor P2P ya está ejecutándose en puerto {}", lifecycleManager.getServerPort());
            return true;
        }
        
        // Start the server
        if (!lifecycleManager.startServer()) {
            return false;
        }
        
        int peerPort = lifecycleManager.getServerPort();
        
        // Configure callbacks
        callbackConfigurator.configureServerCallbacks(lifecycleManager.getServer(), peerPort);
        
        // Establish local server identity
        this.localServerId = identityProvider.resolveLocalServerId(peerPort);
        userSyncManager.setLocalServerId(this.localServerId);
        userReplicationManager.setLocalServerId(this.localServerId);
        peerReplicationManager.setLocalServerId(this.localServerId);
        
        // Set local port in handlers for self-connection prevention
        outgoingHandler.setLocalServerPort(peerPort);
        discoveryHandler.setLocalServerPort(peerPort);
        
        // Configure server in connection manager
        connectionManager.setPeerServer(lifecycleManager.getServer());
        
        logger.info("Servidor P2P iniciado exitosamente en puerto {}", peerPort);
        
        // Attempt reconnection to known peers
        autoReconnectService.reconnectToKnownPeers(
            this.localServerId, 
            connectionManager::isConnectedToPeer
        );
        
        return true;
    }
    
    @Override
    public int getPeerServerPort() {
        return lifecycleManager.getServerPort();
    }
    
    @Override
    public void stopPeerServer() {
        logger.info("Deteniendo servidor P2P y todas las conexiones...");
        
        connectionManager.disconnectAllPeers();
        lifecycleManager.stopServer();
        
        logger.info("Servidor P2P detenido completamente");
    }
    
    @Override
    public boolean connectToPeer(String ip, int port) {
        return outgoingHandler.connectToPeer(ip, port);
    }
    
    @Override
    public boolean disconnectFromPeer(String peerId) {
        boolean success = connectionManager.disconnectFromPeer(peerId);
        
        ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(peerId);
        if (peerInfo != null) {
            observerNotifier.notifyPeerDisconnected(peerInfo.withStatus("Desconectado"));
        } else {
            try {
                String ip = PeerIdParser.extractIp(peerId);
                int port = PeerIdParser.extractPort(peerId);
                if (ip != null && port != -1) {
                    ConnectedPeerInfo basicInfo = ConnectedPeerInfo.create(ip, port, "Desconectado");
                    observerNotifier.notifyPeerDisconnected(basicInfo);
                }
            } catch (Exception e) {
                logger.warn("No se pudo notificar desconexión de {}", peerId);
            }
        }
        userSyncManager.clearPeerUsers(peerId);
        
        return success;
    }
    
    @Override
    public int disconnectFromPeers(List<String> peerIds) {
        return connectionManager.disconnectFromPeers(peerIds);
    }
    
    @Override
    public List<ConnectedPeerInfo> getConnectedPeers() {
        return connectionManager.getConnectedPeers();
    }
    
    @Override
    public boolean isPeerServerRunning() {
        return lifecycleManager.isRunning();
    }
    
    @Override
    public int getCurrentPeerConnections() {
        return connectionManager.getCurrentPeerConnections();
    }
    
    @Override
    public boolean isConnectedToPeer(String peerId) {
        return connectionManager.isConnectedToPeer(peerId);
    }

    @Override
    public boolean sendMessageToPeer(String peerId, String message) {
        return connectionManager.sendMessageToPeer(peerId, message);
    }
    
    @Override
    public int broadcastToPeers(String message) {
        return connectionManager.broadcastToPeers(message);
    }

    @Override
    public void addPeerConnectionObserver(PeerConnectionObserver observer) {
        observerNotifier.addObserver(observer);
    }
    
    @Override
    public void removePeerConnectionObserver(PeerConnectionObserver observer) {
        observerNotifier.removeObserver(observer);
    }

    @Override
    public void notifyUserChangeToPeers(String username, String action) {
        boolean hasPeers = connectionManager.hasPeersConnected();
        userSyncManager.notifyUserChangeToPeers(username, action, hasPeers);
    }
    
    @Override
    public Map<String, List<String>> getAllUsersAcrossPeers() {
        return userSyncManager.getAllUsersAcrossPeers();
    }

    public String getRemoteUserPhoto(String username) {
        return userSyncManager.getRemoteUserPhoto(username);
    }

    public void setClientBroadcastCallback(Consumer<String> callback) {
        userSyncManager.setClientBroadcastCallback(callback);
    }

    public void setClientMessageBroadcaster(ClientMessageBroadcaster broadcaster) {
        messageRoutingManager.setClientBroadcaster(broadcaster);
        logger.info("ClientMessageBroadcaster configurado en PeerMessageRoutingManager");
    }

    public boolean routePrivateMessageToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routePrivateMessageToPeer(recipientUsername, routeMessage);
    }

    public boolean routePrivateAudioToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routePrivateAudioToPeer(recipientUsername, routeMessage);
    }

    public boolean routeChannelInviteToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routeChannelInviteToPeer(recipientUsername, routeMessage);
    }

    public void routeChannelMessageToPeer(String recipientUsername, String routeMessage) {
        messageRoutingManager.routeChannelMessageToPeer(recipientUsername, routeMessage);
    }

    public boolean isUserConnected(String username) {
        logger.debug("Buscando usuario: '{}'", username);

        String actualUsername = username;
        if (username.contains(" - ")) {
            String[] parts = username.split(" - ", 2);
            if (parts[0].startsWith("Servidor ")) {
                actualUsername = parts[1];
                logger.debug("Prefijo remoto detectado. Username original: '{}', username real: '{}'", 
                           username, actualUsername);
            }
        }

        try {
            ActiveUserManager aum = ActiveUserManager.getInstance();
            List<User> localSessions = aum.getUserSessions(actualUsername);
            if (localSessions != null && !localSessions.isEmpty()) {
                logger.info("Usuario '{}' encontrado localmente", actualUsername);
                return true;
            }
        } catch (Exception e) {
            logger.debug("Error verificando usuario local: {}", e.getMessage());
        }

        Map<String, List<String>> remoteUsers = userSyncManager.getAllUsersAcrossPeers();
        logger.debug("Verificando usuario '{}' en {} peers remotos", actualUsername, remoteUsers.size());
        for (Map.Entry<String, List<String>> entry : remoteUsers.entrySet()) {
            logger.debug("Peer {}: {} usuarios -> {}", entry.getKey(), entry.getValue().size(), entry.getValue());
            if (entry.getValue().contains(actualUsername)) {
                logger.info("Usuario '{}' encontrado en peer remoto {}", actualUsername, entry.getKey());
                return true;
            }
        }

        logger.info("Usuario '{}' no encontrado (ni local ni remoto)", actualUsername);
        return false;
    }
}