package com.serverInfrastructure.adapters.peer.managers;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import com.serverInfrastructure.network.peerTcp.PeerTcpClient;
import com.serverInfrastructure.network.peerTcp.PeerTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PeerConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(PeerConnectionManager.class);

    private final Map<String, PeerTcpClient> peerClients = new ConcurrentHashMap<>();
    private final Map<String, ConnectedPeerInfo> connectedPeers = new ConcurrentHashMap<>();
    private PeerTcpServer peerServer;
    
    private PeerObserverNotifier observerNotifier;
    private PeerUserSyncManager userSyncManager;

    public void setPeerServer(PeerTcpServer peerServer) {
        this.peerServer = peerServer;
        this.peerServer.setOnIncomingPeerDisconnected(this::handleDisconnection);
    }
    
    public void setObserverNotifier(PeerObserverNotifier notifier) {
        this.observerNotifier = notifier;
    }

    public void setUserSyncManager(PeerUserSyncManager userSyncManager) {
        this.userSyncManager = userSyncManager;
    }

    public boolean connectToPeer(
        String ip,
        int port,
        Runnable onConnectionRejected,
        Consumer<String> onUserSyncReceived,
        Consumer<String> onPeerListReceived,
        BiConsumer<String, String> onPrivateMessageReceived,
        Consumer<String> onConnectionSuccess
    ) {
        String peerId = PeerIdParser.create(ip, port);
        
        if (peerClients.containsKey(peerId)) {
            logger.warn("Ya existe conexión con peer {}", peerId);
            return false;
        }
        
        try {
            logger.info("Conectando a peer {}...", peerId);
            
            PeerTcpClient client = new PeerTcpClient();
            client.setOnDisconnected(() -> handleDisconnection(peerId));
            client.setOnConnectionRejected(() -> {
                logger.error("Conexión rechazada por peer {} - limpiando registros", peerId);
                peerClients.remove(peerId);
                connectedPeers.remove(peerId);
                onConnectionRejected.run();
            });
            client.setOnUserSyncReceived(onUserSyncReceived);
            client.setOnPeerListReceived(onPeerListReceived);
            client.setOnPrivateMessageReceived(onPrivateMessageReceived);
            
            if (!client.connectToPeer(ip, port)) {
                logger.error("No se pudo conectar a peer {}", peerId);
                return false;
            }
            
            peerClients.put(peerId, client);
            connectedPeers.put(peerId, ConnectedPeerInfo.create(ip, port, "Conectado"));

            try {
                PeerRegistry.getInstance().addKnownPeer(connectedPeers.get(peerId));
            } catch (Exception e) {
                logger.warn("No se pudo persistir peer conocido {}: {}", peerId, e.getMessage());
            }

            client.sendHandshake();

            new Thread(() -> {
                try {
                    Thread.sleep(200);
                    if (peerClients.containsKey(peerId) && client.isConnected()) {
                        onConnectionSuccess.accept(peerId);
                        logger.info("Conectado exitosamente a peer {}", peerId);
                    }
                } catch (Exception e) {
                    logger.error("Error en validación post-conexión: {}", e.getMessage());
                }
            }, "ConnectionValidation-" + peerId).start();
            
            logger.info("Conexión inicial establecida con peer {}, validando...", peerId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error conectando a peer {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    public boolean disconnectFromPeer(String peerId) {
        logger.info("Iniciando desconexión activa del peer {}", peerId);

        PeerTcpClient client = peerClients.get(peerId);
        if (client != null) {
            client.disconnect();
        }

        if (peerServer != null && peerServer.isRunning()) {
            peerServer.disconnectIncomingPeer(peerId);
        }

        if (connectedPeers.containsKey(peerId)) {
            handleDisconnection(peerId);
        }
        
        return true;
    }

    public int disconnectFromPeers(List<String> peerIds) {
        int disconnected = 0;
        for (String peerId : peerIds) {
            if (disconnectFromPeer(peerId)) {
                disconnected++;
            }
        }
        logger.info("Desconectados {} de {} peers solicitados", disconnected, peerIds.size());
        return disconnected;
    }

    public void disconnectAllPeers() {
        List<String> peerIds = new ArrayList<>(peerClients.keySet());
        for (String peerId : peerIds) {
            disconnectFromPeer(peerId);
        }
    }

    private synchronized void handleDisconnection(String peerId) {
        peerClients.remove(peerId);

        ConnectedPeerInfo removedPeerInfo = connectedPeers.remove(peerId);

        if (removedPeerInfo != null) {
            logger.info("Peer {} desconectado. Notificando a los observadores de UI.", peerId);
            if (observerNotifier != null) {
                observerNotifier.notifyPeerDisconnected(removedPeerInfo.withStatus("Desconectado"));
            }
            
            if (userSyncManager != null) {
                userSyncManager.handlePeerDisconnection(peerId);
            }

        } else {
            logger.debug("handleDisconnection llamado para {}, pero ya no estaba en la lista de conectados.", peerId);
        }
    }

    public boolean sendMessageToPeer(String peerId, String message) {
        // First try outgoing connections
        PeerTcpClient client = peerClients.get(peerId);
        if (client != null && client.isConnected()) {
            return client.sendMessage(message);
        }
        
        // Then try incoming connections
        if (peerServer != null && peerServer.isRunning()) {
            if (peerServer.sendMessageToIncomingPeer(peerId, message)) {
                logger.debug("Mensaje enviado a peer entrante {}", peerId);
                return true;
            }
        }
        
        logger.warn("No hay conexión activa con peer {} (ni saliente ni entrante)", peerId);
        return false;
    }

    public int broadcastToPeers(String message) {
        int sent = 0;

        for (Map.Entry<String, PeerTcpClient> entry : peerClients.entrySet()) {
            String peerId = entry.getKey();
            PeerTcpClient client = entry.getValue();
            if (client.isConnected() && client.sendMessage(message)) {
                sent++;
                logger.debug("Mensaje broadcast enviado a peer (saliente) {}", peerId);
            } else {
                logger.warn("No se pudo enviar mensaje broadcast a peer (saliente) {}", peerId);
            }
        }

        if (peerServer != null && peerServer.isRunning()) {
            for (String incomingPeerId : peerServer.listIncomingPeerIds()) {
                if (peerClients.containsKey(incomingPeerId)) {
                    continue;
                }
                if (peerServer.sendMessageToIncomingPeer(incomingPeerId, message)) {
                    sent++;
                    logger.debug("Mensaje broadcast enviado a peer (entrante) {}", incomingPeerId);
                } else {
                    logger.warn("No se pudo enviar mensaje broadcast a peer (entrante) {}", incomingPeerId);
                }
            }
        }

        int totalKnown = peerClients.size() + (peerServer != null ? peerServer.listIncomingPeerIds().size() : 0);
        logger.info("Mensaje broadcast enviado a {} de {} peers", sent, totalKnown);
        return sent;
    }

    public List<ConnectedPeerInfo> getConnectedPeers() {
        return new ArrayList<>(connectedPeers.values());
    }

    public boolean isConnectedToPeer(String peerId) {
        PeerTcpClient client = peerClients.get(peerId);
        return client != null && client.isConnected();
    }

    public int getCurrentPeerConnections() {
        return peerClients.size();
    }

    public int getIncomingPeerCount() {
        if (peerServer != null && peerServer.isRunning()) {
            return peerServer.listIncomingPeerIds().size();
        }
        return 0;
    }

    public boolean hasPeersConnected() {
        return !peerClients.isEmpty() || getIncomingPeerCount() > 0;
    }

    public ConnectedPeerInfo getPeerInfo(String peerId) {
        return connectedPeers.get(peerId);
    }

    public void registerIncomingPeer(ConnectedPeerInfo info) {
        if (info == null) return;
        connectedPeers.put(info.peerId(), info);
        try {
            PeerRegistry.getInstance().addKnownPeer(info);
        } catch (Exception e) {
            logger.warn("No se pudo persistir peer entrante {}: {}", info.peerId(), e.getMessage());
        }
        logger.info("Peer entrante registrado: {}", info.peerId());
    }

    public void handleIncomingPeer(java.net.Socket incomingSocket) {
        if (peerServer == null) {
            logger.error("PeerTcpServer no configurado, no se puede manejar conexión entrante");
            try {
                incomingSocket.close();
            } catch (Exception e) {
                logger.error("Error cerrando socket: {}", e.getMessage());
            }
            return;
        }
        peerServer.handleIncomingPeer(incomingSocket);
    }
}