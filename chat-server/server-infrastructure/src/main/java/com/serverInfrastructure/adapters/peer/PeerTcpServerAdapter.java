package com.serverInfrastructure.adapters.peer;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverApplication.ports.PeerConnectionObserver;
import com.serverApplication.ports.peer.*;
import com.serverInfrastructure.adapters.peer.managers.*;
import com.serverInfrastructure.network.peerTcp.PeerTcpServer;
import com.serverInfrastructure.factories.PeerManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class PeerTcpServerAdapter implements PeerNetworkControl {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerTcpServerAdapter.class);

    private PeerTcpServer peerServer;

    private final PeerConnectionManager connectionManager;
    private final PeerUserSyncManager userSyncManager;
    private final PeerMessageRoutingManager messageRoutingManager;
    private final PeerObserverNotifier observerNotifier;

    public PeerTcpServerAdapter(
            PeerConnectionManager connectionManager,
            PeerUserSyncManager userSyncManager,
            PeerMessageRoutingManager messageRoutingManager,
            PeerObserverNotifier observerNotifier) {
        
        this.connectionManager = connectionManager;
        this.userSyncManager = userSyncManager;
        this.messageRoutingManager = messageRoutingManager;
        this.observerNotifier = observerNotifier;

        configureManagerDependencies();
        
        logger.info("PeerTcpServerAdapter inicializado con managers inyectados");
    }

    public static PeerTcpServerAdapter create() {
        return new PeerTcpServerAdapter(
            PeerManagerFactory.createConnectionManager(),
            PeerManagerFactory.createUserSyncManager(),
            PeerManagerFactory.createMessageRoutingManager(),
            PeerManagerFactory.createObserverNotifier()
        );
    }

    private void configureManagerDependencies() {
        userSyncManager.setPeerBroadcastCallback(connectionManager::broadcastToPeers);
        messageRoutingManager.setRemoteUsersProvider(userSyncManager::getAllUsersAcrossPeers);

        messageRoutingManager.setPeerMessageSender((peerId, message) -> {
            if (!connectionManager.sendMessageToPeer(peerId, message)) {
                if (peerServer != null && peerServer.isRunning()) {
                    peerServer.sendMessageToIncomingPeer(peerId, message);
                }
            }
        });

        userSyncManager.setLocalUsersProvider(this::getLocalConnectedUsers);
    }

    @Override
    public boolean startPeerServer() {
        if (peerServer != null && peerServer.isRunning()) {
            logger.warn("Servidor P2P ya está ejecutándose en puerto {}", peerServer.getPeerPort());
            return true;
        }
        
        try {
            peerServer = new PeerTcpServer();
            int peerPort = peerServer.getPeerServerPort();
            
            peerServer.setOnUserSyncReceived((peerId, message) -> {
                logger.debug("Procesando sincronización de usuarios de peer entrante {}", peerId);
                userSyncManager.handleUserSyncMessage(peerId, message);
            });
            
            peerServer.setOnGetLocalUserSync(() -> userSyncManager.generateInitialSyncMessage(peerPort));
            
            peerServer.setOnPrivateMessageReceived((peerId, message) -> {
                if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                    logger.info("Procesando audio privado enrutado de peer {}", peerId);
                    messageRoutingManager.handlePrivateAudioRouted(peerId, message);
                } else {
                    logger.info("Procesando mensaje privado enrutado de peer {}", peerId);
                    messageRoutingManager.handlePrivateMessageRouted(peerId, message);
                }
            });
            
            peerServer.startPeerServer();

            String localServerId = establishLocalServerId(peerPort);
            userSyncManager.setLocalServerId(localServerId);

            connectionManager.setPeerServer(peerServer);
            
            logger.info("Servidor P2P iniciado exitosamente en puerto {}", peerPort);
            return true;
            
        } catch (Exception e) {
            logger.error("Error iniciando servidor P2P: {}", e.getMessage());
            peerServer = null;
            return false;
        }
    }
    
    @Override
    public int getPeerServerPort() {
        if (peerServer != null) {
            return peerServer.getPeerServerPort();
        }
        return new PeerTcpServer().getPeerServerPort();
    }

    private String establishLocalServerId(int peerPort) {
        try {
            String localIp = InetAddress.getLocalHost().getHostAddress();
            String serverId = localIp + ":" + peerPort;
            logger.info("ID del servidor local: {}", serverId);
            return serverId;
        } catch (Exception e) {
            String serverId = "127.0.0.1:" + peerPort;
            logger.warn("No se pudo obtener IP local, usando: {}", serverId);
            return serverId;
        }
    }
    
    @Override
    public void stopPeerServer() {
        logger.info("Deteniendo servidor P2P y todas las conexiones...");
        
        connectionManager.disconnectAllPeers();
        
        if (peerServer != null) {
            peerServer.stopPeerServer();
            peerServer = null;
        }
        
        logger.info("Servidor P2P detenido completamente");
    }
    
    @Override
    public boolean connectToPeer(String ip, int port) {
        String peerId = ip + ":" + port;
        
        return connectionManager.connectToPeer(ip, port, () -> {
                ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(peerId);
                if (peerInfo != null) {
                    observerNotifier.notifyPeerDisconnected(peerInfo.withStatus("Conexión Rechazada"));
                }
                observerNotifier.notifyPeerConnectionError(peerId, "Conexión rechazada - intento de conectar P2P al puerto de clientes");
            },

            message -> {
                logger.debug("Listado de usuarios actualizado por peer {}", peerId);
                userSyncManager.handleUserSyncMessage(peerId, message);
            },


            (sourcePeerId, message) -> {
                if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                    logger.info("Recibiendo audio privado enrutado mediante peer {}", sourcePeerId);
                    messageRoutingManager.handlePrivateAudioRouted(sourcePeerId, message);
                } else {
                    logger.info("Recibiendo mensaje privado enrutado mediante peer {}", sourcePeerId);
                    messageRoutingManager.handlePrivateMessageRouted(sourcePeerId, message);
                }
            },

            confirmedPeerId -> {
                ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(confirmedPeerId);
                if (peerInfo != null) {
                    observerNotifier.notifyPeerConnected(peerInfo);
                }

                userSyncManager.sendFullUserSyncToPeer(confirmedPeerId, 
                    message -> connectionManager.sendMessageToPeer(confirmedPeerId, message));
            }
        );
    }
    
    @Override
    public boolean disconnectFromPeer(String peerId) {
        boolean success = connectionManager.disconnectFromPeer(peerId);
        
        ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(peerId);
        if (peerInfo != null) {
            observerNotifier.notifyPeerDisconnected(peerInfo.withStatus("Desconectado"));
        } else {
            try {
                ConnectedPeerInfo basicInfo = ConnectedPeerInfo.create(
                    peerId.split(":")[0],
                    Integer.parseInt(peerId.split(":")[1]),
                    "Desconectado"
                );
                observerNotifier.notifyPeerDisconnected(basicInfo);
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
        return peerServer != null && peerServer.isRunning();
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

    public void setClientBroadcastCallback(Consumer<String> callback) {
        ClientMessageBroadcaster broadcaster =
            (targetUsername, message, excludeConnectionId) -> callback.accept(message);
        
        userSyncManager.setClientBroadcastCallback(callback);
        messageRoutingManager.setClientBroadcaster(broadcaster);
        logger.info("ClientMessageBroadcaster configurado en managers");
    }

    private List<String> getLocalConnectedUsers() {
        try {
            com.serverInfrastructure.observers.ActiveUserManager userManager = 
                com.serverInfrastructure.observers.ActiveUserManager.getInstance();
            return new ArrayList<>(userManager.getAllUserSessions().keySet());
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios locales: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // Enrutamiento de mensajes privados de cliente a cliente mediante servidores: ClienteA -> ServerA -> ServerB -> ClientB
    public boolean routePrivateMessageToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routePrivateMessageToPeer(recipientUsername, routeMessage);
    }

    public boolean routePrivateAudioToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routePrivateAudioToPeer(recipientUsername, routeMessage);
    }
}
