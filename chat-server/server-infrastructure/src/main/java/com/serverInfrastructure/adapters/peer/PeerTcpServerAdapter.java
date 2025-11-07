package com.serverInfrastructure.adapters.peer;
import com.serverInfrastructure.utils.NetworkUtils;
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

    private String localServerId;

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

        this.connectionManager.setObserverNotifier(observerNotifier);
        
        this.connectionManager.setUserSyncManager(userSyncManager);
        
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

            // Notificar y registrar peers entrantes en los managers cuando se acepta la conexión
            peerServer.setOnPeerConnected(info -> {
                try {
                    // Only accept/register incoming peers that advertise a non-ephemeral port
                    if (info == null) {
                        logger.debug("OnPeerConnected received null info, ignoring");
                        return;
                    }
                    // parse the peer port from info
                    String pid = info.peerId();
                    int remotePort = -1;
                    try {
                        String[] parts = pid.split(":");
                        if (parts.length > 1) remotePort = Integer.parseInt(parts[1]);
                    } catch (Exception ex) {
                        logger.debug("No se pudo parsear puerto de peerId {}: {}", pid, ex.getMessage());
                    }

                    // Allow incoming peers if they advertise a non-ephemeral port.
                    // Previously we required exact port equality which prevented registering valid peers
                    // that run their P2P server on a different port. Only reject typical ephemeral ports.
                    int EPHEMERAL_LOWER = 49152;
                    int EPHEMERAL_UPPER = 65535;
                    if (remotePort >= EPHEMERAL_LOWER && remotePort <= EPHEMERAL_UPPER) {
                        logger.debug("Ignorando conexión entrante desde puerto efímero {} (esperado no-efímero)", remotePort);
                        return;
                    }

                    connectionManager.registerIncomingPeer(info);
                } catch (Exception e) {
                    logger.warn("No se pudo registrar peer entrante en connectionManager: {}", e.getMessage());
                }
                try {
                    observerNotifier.notifyPeerConnected(info);
                } catch (Exception e) {
                    logger.warn("No se pudo notificar observadores de peer conectado: {}", e.getMessage());
                }
            });
            
            peerServer.setOnGetLocalUserSync(() -> userSyncManager.generateInitialSyncMessage(peerPort));
            
            peerServer.setOnPrivateMessageReceived((peerId, message) -> {
                if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                    logger.info("Procesando audio privado enrutado de peer {}", peerId);
                    messageRoutingManager.handlePrivateAudioRouted(peerId, message);
                } else if (message.startsWith("P2P_ROUTE_PRIVATE")) {
                    logger.info("Procesando mensaje privado enrutado de peer {}", peerId);
                    messageRoutingManager.handlePrivateMessageRouted(peerId, message);
                } else if (message.startsWith("P2P_CHANNEL_INVITE")) {
                    logger.info("Procesando invitación de canal enrutada de peer {}", peerId);
                    messageRoutingManager.handleChannelInviteRouted(peerId, message);
                } else if (message.startsWith("P2P_CHANNEL_MESSAGE")) {
                    logger.info("Procesando mensaje de canal enrutado de peer {}", peerId);
                    messageRoutingManager.handleChannelMessageRouted(peerId, message);
                }
                // Nota: P2P_USER_SYNC se maneja en setOnUserSyncReceived
            });
            
            peerServer.startPeerServer();

            this.localServerId = establishLocalServerId(peerPort);
            userSyncManager.setLocalServerId(this.localServerId);

            connectionManager.setPeerServer(peerServer);
            
            logger.info("Servidor P2P iniciado exitosamente en puerto {}", peerPort);
            // Intentar reconectar a peers persistidos al iniciar
            try {
                com.serverInfrastructure.adapters.peer.managers.PeerRegistry registry =
                        com.serverInfrastructure.adapters.peer.managers.PeerRegistry.getInstance();
                for (String known : registry.getKnownPeers()) {
                    try {
                        if (known == null || known.isBlank()) continue;
                        if (known.equals(this.localServerId)) continue;
                        String[] parts = known.split(":");
                        if (parts.length != 2) continue;
                        String kip = parts[0];
                        int kport = Integer.parseInt(parts[1]);
                        if (!connectionManager.isConnectedToPeer(known)) {
                            logger.info("Reconectando a peer persistido {}", known);
                            // use adapter-level connect to reuse callbacks
                            connectToPeer(kip, kport);
                        }
                    } catch (Exception ex) {
                        logger.warn("No se pudo reconectar peer persistido {}: {}", known, ex.getMessage());
                    }
                }
            } catch (Exception e) {
                logger.debug("No se pudo iniciar reconexión a peers conocidos: {}", e.getMessage());
            }
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

        if (peerServer != null && NetworkUtils.isLocalAddress(ip, port, peerServer.getPeerPort())) {
            logger.info("Auto-conexión detectada y prevenida para {}:{}", ip, port);
            return false;
        }
        
        return connectionManager.connectToPeer(
            ip, 
            port, 
            // 1. onConnectionRejected
            () -> {
                ConnectedPeerInfo peerInfo = connectionManager.getPeerInfo(peerId);
                if (peerInfo != null) {
                    observerNotifier.notifyPeerDisconnected(peerInfo.withStatus("Conexión Rechazada"));
                }
                observerNotifier.notifyPeerConnectionError(peerId, "Conexión rechazada - intento de conectar P2P al puerto de clientes");
            },

            // 2. onUserSyncReceived
            message -> {
                logger.debug("Listado de usuarios actualizado por peer {}", peerId);
                userSyncManager.handleUserSyncMessage(peerId, message);
            },

            // 3. onPeerListReceived - Manejar lista de peers recibida desde un peer
            peerListMessage -> {
                try {
                    if (peerListMessage == null || !peerListMessage.contains("peers=")) return;
                    String after = peerListMessage.substring(peerListMessage.indexOf("peers=") + 6);
                    String[] items = after.split(",");
                    for (String item : items) {
                        String p = item.trim();
                        if (p.isEmpty()) continue;

                        // Lógica simplificada y robusta
                        try {
                            String[] parts = p.split(":");
                            if (parts.length != 2) continue;
                            String discoveredIp = parts[0];
                            int discoveredPort = Integer.parseInt(parts[1]);

                            // ¡Usa nuestra nueva utilidad para la comprobación!
                            if (peerServer != null && NetworkUtils.isLocalAddress(discoveredIp, discoveredPort, peerServer.getPeerPort())) {
                                logger.debug("Omitiendo peer descubierto que corresponde al servidor local: {}", p);
                                continue;
                            }
                            
                            // Evitar conectar al peer que nos envió la lista (sigue siendo útil)
                            if (p.equals(peerId)) continue;

                            // Intentar conectar (en background para no bloquear)
                            new Thread(() -> {
                                try {
                                    logger.info("Descubierto peer {} desde {}, intentando conectar...", p, peerId);
                                    connectToPeer(discoveredIp, discoveredPort);
                                } catch (Exception ex) {
                                    logger.warn("Error intentando conectar peer descubierto {}: {}", p, ex.getMessage());
                                }
                            }, "PeerAutoConnect-" + p).start();
                        } catch (Exception ex) {
                            logger.warn("Error procesando peer descubierto '{}': {}", p, ex.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Error procesando peerList de {}: {}", peerId, e.getMessage());
                }
            },

            // 4. onPrivateMessageReceived
            (sourcePeerId, message) -> {
                if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                    logger.info("Recibiendo audio privado enrutado mediante peer {}", sourcePeerId);
                    messageRoutingManager.handlePrivateAudioRouted(sourcePeerId, message);
                } else if (message.startsWith("P2P_ROUTE_PRIVATE")) {
                    logger.info("Recibiendo mensaje privado enrutado mediante peer {}", sourcePeerId);
                    messageRoutingManager.handlePrivateMessageRouted(sourcePeerId, message);
                } else if (message.startsWith("P2P_CHANNEL_INVITE")) {
                    logger.info("Recibiendo invitación de canal enrutada mediante peer {}", sourcePeerId);
                    messageRoutingManager.handleChannelInviteRouted(sourcePeerId, message);
                } else if (message.startsWith("P2P_CHANNEL_MESSAGE")) {
                    logger.info("Recibiendo mensaje de canal enrutado mediante peer {}", sourcePeerId);
                    messageRoutingManager.handleChannelMessageRouted(sourcePeerId, message);
                }
            },

            // 5. onConnectionSuccess
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

    /**
     * Enruta una invitación de canal a un usuario remoto.
     */
    public boolean routeChannelInviteToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routeChannelInviteToPeer(recipientUsername, routeMessage);
    }

    /**
     * Enruta un mensaje de canal a un usuario remoto.
     */
    public boolean routeChannelMessageToPeer(String recipientUsername, String routeMessage) {
        return messageRoutingManager.routeChannelMessageToPeer(recipientUsername, routeMessage);
    }

    /**
     * Verifica si un usuario está conectado localmente o en un peer remoto.
     */
    public boolean isUserConnected(String username) {
        // Verificar usuarios locales
        try {
            com.serverInfrastructure.observers.ActiveUserManager aum = 
                com.serverInfrastructure.observers.ActiveUserManager.getInstance();
            List<com.serverDomain.entities.User> localSessions = aum.getUserSessions(username);
            if (localSessions != null && !localSessions.isEmpty()) {
                return true;
            }
        } catch (Exception e) {
            logger.debug("Error verificando usuario local: {}", e.getMessage());
        }

        // Verificar usuarios remotos
        Map<String, List<String>> remoteUsers = userSyncManager.getAllUsersAcrossPeers();
        for (List<String> users : remoteUsers.values()) {
            if (users.contains(username)) {
                return true;
            }
        }

        return false;
    }
}
