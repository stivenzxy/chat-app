package com.serverInfrastructure.adapters.peer.managers;

import com.serverApplication.dto.ConnectedPeerInfo;
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

    public void setPeerServer(PeerTcpServer peerServer) {
        this.peerServer = peerServer;
    }
    

    public boolean connectToPeer(
            String ip, 
            int port,
            Runnable onConnectionRejected,
            Consumer<String> onUserSyncReceived,
            BiConsumer<String, String> onPrivateMessageReceived,
            Consumer<String> onConnectionSuccess
    ) {
        String peerId = ip + ":" + port;
        
        if (peerClients.containsKey(peerId)) {
            logger.warn("Ya existe conexión con peer {}", peerId);
            return false;
        }
        
        try {
            logger.info("Conectando a peer {}...", peerId);
            
            PeerTcpClient client = new PeerTcpClient();

            client.setOnConnectionRejected(() -> {
                logger.error("Conexión rechazada por peer {} - limpiando registros", peerId);
                peerClients.remove(peerId);
                connectedPeers.remove(peerId);
                onConnectionRejected.run();
            });
            
            client.setOnUserSyncReceived(onUserSyncReceived);
            client.setOnPrivateMessageReceived(onPrivateMessageReceived);
            
            boolean connected = client.connectToPeer(ip, port);
            
            if (connected) {
                peerClients.put(peerId, client);

                ConnectedPeerInfo peerInfo = ConnectedPeerInfo.create(ip, port, "Conectado");
                connectedPeers.put(peerId, peerInfo);

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
                
            } else {
                logger.error("No se pudo conectar a peer {}", peerId);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error conectando a peer {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    public boolean disconnectFromPeer(String peerId) {
        PeerTcpClient client = peerClients.get(peerId);
        if (client != null) {
            try {
                client.disconnect();
                peerClients.remove(peerId);
                logger.info("Conexión como cliente a {} cerrada", peerId);
            } catch (Exception e) {
                logger.error("Error cerrando conexión de cliente P2P a {}: {}", peerId, e.getMessage());
            }
        }

        if (peerServer != null && peerServer.isRunning()) {
            if (peerServer.disconnectIncomingPeer(peerId)) {
                logger.info("Conexión como servidor a {} cerrada", peerId);
            }
        }

        ConnectedPeerInfo peerInfo = connectedPeers.remove(peerId);
        
        if (peerInfo != null) {
            logger.info("Limpiando registro de peer {}", peerId);
        }
        
        boolean success = true;

        logger.info("Desconexión completa de peer {}", peerId);

        return success;
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

    public boolean sendMessageToPeer(String peerId, String message) {
        PeerTcpClient client = peerClients.get(peerId);
        if (client == null || !client.isConnected()) {
            logger.warn("No hay conexión activa con peer {}", peerId);
            return false;
        }
        return client.sendMessage(message);
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