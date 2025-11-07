package com.serverInfrastructure.network.peerTcp;

import com.chatCommon.utils.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.function.Consumer;

public class PeerTcpServer {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerTcpServer.class);
    
    private ServerSocket peerServerSocket;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private Thread acceptorThread;
    private final int peerPort;

    private final Map<String, Socket> incomingPeerSockets = new ConcurrentHashMap<>();
    private final Map<String, Thread> incomingPeerThreads = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> incomingPeerWriters = new ConcurrentHashMap<>();

    private BiConsumer<String, String> onUserSyncReceived;

    private Supplier<String> onGetLocalUserSync;

    private BiConsumer<String, String> onPrivateMessageReceived;
    private java.util.function.Consumer<com.serverApplication.dto.ConnectedPeerInfo> onPeerConnected;
    private Consumer<String> onIncomingPeerDisconnected;



    public PeerTcpServer() {
        AppProperties props = new AppProperties("server-configuration");
        this.peerPort = props.getInt("PEER_SERVER_PORT");
        logger.info("PeerTcpServer inicializado - Puerto P2P configurado: {}", peerPort);
    }

    public void setOnUserSyncReceived(BiConsumer<String, String> callback) {
        this.onUserSyncReceived = callback;
    }

    public void setOnGetLocalUserSync(Supplier<String> callback) {
        this.onGetLocalUserSync = callback;
    }

    public void setOnPrivateMessageReceived(BiConsumer<String, String> callback) {
        this.onPrivateMessageReceived = callback;
    }

    public void setOnPeerConnected(java.util.function.Consumer<com.serverApplication.dto.ConnectedPeerInfo> callback) {
        this.onPeerConnected = callback;
    }

    public boolean sendMessageToIncomingPeer(String peerId, String message) {
        PrintWriter writer = incomingPeerWriters.get(peerId);
        if (writer == null) {
            return false;
        }
        try {
            writer.println(message);
            return !writer.checkError();
        } catch (Exception e) {
            logger.warn("No se pudo enviar mensaje a peer entrante {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    public List<String> listIncomingPeerIds() {
        return new ArrayList<>(incomingPeerWriters.keySet());
    }

    /**
     * Inicia el servidor P2P en el puerto configurado (PEER_SERVER_PORT).
     * Se llama automáticamente cuando inicia el servidor principal de clientes.
     */
    public void startPeerServer() {
        if (isRunning.get()) {
            logger.warn("PeerTcpServer ya está ejecutándose en puerto {}", this.peerPort);
            return;
        }
        
        try {
            peerServerSocket = new ServerSocket(peerPort);
            isRunning.set(true);
            
            logger.info("PeerTcpServer iniciado en puerto {} - Esperando conexiones de otros servidores", peerPort);

            acceptorThread = new Thread(this::acceptPeerConnections, "PeerServer-Acceptor");
            acceptorThread.start();
            
        } catch (IOException e) {
            logger.error("Error al iniciar PeerTcpServer en puerto {}: {}", peerPort, e.getMessage());
            isRunning.set(false);
        }
    }
    
    /**
     * Obtiene el puerto P2P configurado.
     */
    public int getPeerServerPort() {
        return peerPort;
    }
    

    private void acceptPeerConnections() {
        logger.info("PeerTcpServer comenzó a escuchar conexiones entrantes");
        
        while (isRunning.get()) {
            try {
                Socket incomingPeerSocket = peerServerSocket.accept();
                
                String peerIp = incomingPeerSocket.getInetAddress().getHostAddress();
                int peerPort = incomingPeerSocket.getPort();
                
                logger.info("Nueva conexión P2P entrante desde {}:{}", peerIp, peerPort);
                
                // Manejar conexión entrante (validación handshake, almacenamiento, comunicación)
                handleIncomingPeer(incomingPeerSocket);
                
            } catch (SocketException e) {
                if (isRunning.get()) {
                    logger.error("SocketException en PeerTcpServer: {}", e.getMessage());
                }
            } catch (IOException e) {
                if (isRunning.get()) {
                    logger.error("Error aceptando conexión P2P: {}", e.getMessage());
                }
            } catch (Exception e) {
                logger.error("Error inesperado en PeerTcpServer: {}", e.getMessage(), e);
            }
        }
        
        logger.info("PeerTcpServer detuvo el loop de aceptación");
    }

    public void handleIncomingPeer(java.net.Socket peerSocket) {
        simulateIncomingPeerHandling(peerSocket);
    }

     public void setOnIncomingPeerDisconnected(Consumer<String> callback) {
        this.onIncomingPeerDisconnected = callback;
    }

    private void simulateIncomingPeerHandling(Socket peerSocket) {
        Thread peerHandler = new Thread(() -> {
            BufferedReader input = null;
            PrintWriter output = null;
            String peerId = null;
            
            try {
                String tempId = peerSocket.getInetAddress().getHostAddress() + ":" + peerSocket.getPort();
                logger.info("Evaluando conexión entrante: {}", tempId);

                input = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()));
                output = new PrintWriter(peerSocket.getOutputStream(), true);

                peerSocket.setSoTimeout(5000);
                String handshakeMessage = input.readLine();
                
                if (handshakeMessage == null) {
                    logger.warn("Conexión cerrada antes del handshake: {}", peerId);
                    return;
                }

                if (!handshakeMessage.startsWith("P2P_SERVER_HANDSHAKE|")) {
                    logger.warn("Rechazando conexión no-P2P desde {}: {}", tempId, handshakeMessage.substring(0, Math.min(50, handshakeMessage.length())));

                    output.println("P2P_CONNECTION_REJECTED|reason=NOT_P2P_SERVER|message=Este puerto es para conexiones P2P entre servidores");
                    peerSocket.close();
                    return;
                }

                peerId = extractPeerIdFromHandshake(handshakeMessage);
                if (peerId == null) {
                    peerId = tempId;
                }
                
                logger.info("Handshake P2P válido recibido de {}", peerId);

                incomingPeerSockets.put(peerId, peerSocket);
                incomingPeerThreads.put(peerId, Thread.currentThread());
                logger.info("Conexión P2P entrante almacenada: {}", peerId);

                // Acknowledge and advertise the server's configured P2P listening port (this.peerPort)
                output.println("P2P_SERVER_HANDSHAKE_ACK|myId=" + getLocalIp() + ":" + this.peerPort + "|status=ACCEPTED");

                incomingPeerWriters.put(peerId, output);

                if (onGetLocalUserSync != null) {
                    try {
                        String localUserSyncMessage = onGetLocalUserSync.get();
                        if (localUserSyncMessage != null && !localUserSyncMessage.trim().isEmpty()) {
                            output.println(localUserSyncMessage);
                            logger.info("Sincronización inicial enviada a peer conectado {}", peerId);
                        }
                    } catch (Exception e) {
                        logger.error("Error enviando sincronización inicial a {}: {}", peerId, e.getMessage());
                    }
                }

                // Registrar peer entrante como conocido SOLO si el puerto coincide con PEER_SERVER_PORT
                try {
                    com.serverInfrastructure.adapters.peer.managers.PeerRegistry registry = com.serverInfrastructure.adapters.peer.managers.PeerRegistry.getInstance();
                    if (registry != null) {
                        registry.addKnownPeer(peerId); // registry will filter invalid ports
                    }
                } catch (Exception e) {
                    logger.warn("No se pudo registrar peer entrante {}: {}", peerId, e.getMessage());
                }

                // Enviar lista de peers conocidos al peer entrante (solo válidos)
                try {
                    com.serverInfrastructure.adapters.peer.managers.PeerRegistry registry = com.serverInfrastructure.adapters.peer.managers.PeerRegistry.getInstance();
                    java.util.Set<String> peers = registry.getValidPeers();
                    if (!peers.isEmpty()) {
                        String payload = String.join(",", peers);
                        output.println("P2P_PEER_LIST|peers=" + payload);
                        logger.info("Enviada P2P_PEER_LIST a peer {}: {} peers", peerId, peers.size());
                    }
                } catch (Exception e) {
                    logger.debug("No se pudo enviar peer list al entrante {}: {}", peerId, e.getMessage());
                }

                // Notify upper layers about the new incoming peer so they can register it
                try {
                    if (onPeerConnected != null) {
                        // create a ConnectedPeerInfo parsing the peerId (format ip:port)
                        String[] parts = peerId.split(":" );
                        String ip = parts.length > 0 ? parts[0] : peerSocket.getInetAddress().getHostAddress();
                        int port = 0;
                        try { port = parts.length > 1 ? Integer.parseInt(parts[1]) : peerSocket.getPort(); } catch (Exception ignore) { port = peerSocket.getPort(); }
                        com.serverApplication.dto.ConnectedPeerInfo info = com.serverApplication.dto.ConnectedPeerInfo.create(ip, port, "Conectado");
                        onPeerConnected.accept(info);
                    }
                } catch (Exception e) {
                    logger.warn("Error notificando peer entrante a capas superiores: {}", e.getMessage());
                }

                peerSocket.setSoTimeout(0);

                String message;
                while (isRunning.get() && !peerSocket.isClosed() && (message = input.readLine()) != null) {
                    if ("DISCONNECT".equals(message)) {
                        logger.info("Peer {} solicita desconexión", peerId);
                        break;
                    }
                    
                    if (message.startsWith("P2P_USER_SYNC")) {
                        logger.debug("Sincronización de usuarios de peer {}", peerId);
                        if (onUserSyncReceived != null) {
                            onUserSyncReceived.accept(peerId, message);
                        }
                    } else if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
                        logger.info("(SERVER) Audio privado enrutado recibido de peer {}", peerId);
                        if (onPrivateMessageReceived != null) {
                            onPrivateMessageReceived.accept(peerId, message);
                        }
                    } else if (message.startsWith("P2P_ROUTE_PRIVATE")) {
                        logger.info("(SERVER) Mensaje privado enrutado recibido de peer {}", peerId);
                        if (onPrivateMessageReceived != null) {
                            onPrivateMessageReceived.accept(peerId, message);
                        }
                    }
                    
                    logger.debug("Mensaje de peer {}: {}", peerId, message);
                }
                
            } catch (Exception e) {
                logger.error("Error manejando peer entrante: {}", e.getMessage());
            } finally {
                if (peerId != null) {
                    incomingPeerSockets.remove(peerId);
                    incomingPeerThreads.remove(peerId);
                    incomingPeerWriters.remove(peerId);
                    
                    // ¡Notificar que este peer entrante se ha desconectado!
                    if (onIncomingPeerDisconnected != null) {
                        onIncomingPeerDisconnected.accept(peerId);
                    }
                    
                    logger.info("Conexión P2P entrante removida y notificada: {}", peerId);
                }
                
                try {
                    if (input != null) input.close();
                    if (output != null) output.close();
                    if (!peerSocket.isClosed()) peerSocket.close();
                    logger.info("Conexión P2P entrante cerrada");
                } catch (IOException e) {
                    logger.error("Error cerrando socket de peer: {}", e.getMessage());
                }
            }
        }, "IncomingPeerHandler-" + peerSocket.getInetAddress().getHostAddress());
        
        peerHandler.start();
    }

    private String extractPeerIdFromHandshake(String handshake) {
        try {
            for (String part : handshake.split("\\|")) {
                if (part.startsWith("myId=")) {
                    return part.substring(5);
                }
            }
        } catch (Exception e) {
            logger.warn("No se pudo extraer peerId del handshake: {}", e.getMessage());
        }
        return null;
    }

    public boolean disconnectIncomingPeer(String peerId) {
        Socket socket = incomingPeerSockets.get(peerId);
        Thread thread = incomingPeerThreads.get(peerId);
        
        if (socket == null) {
            logger.info("No hay conexión entrante activa con peer {}", peerId);
            return false;
        }
        
        try {
            logger.info("Cerrando conexión entrante de peer {}", peerId);

            socket.close();

            if (thread != null && thread.isAlive()) {
                thread.interrupt();
            }

            incomingPeerSockets.remove(peerId);
            incomingPeerThreads.remove(peerId);
            
            logger.info("Conexión entrante de peer {} cerrada", peerId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error cerrando conexión entrante de peer {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    public void stopPeerServer() {
        if (!isRunning.get()) {
            logger.info("PeerTcpServer ya está detenido");
            return;
        }
        
        logger.info("Deteniendo PeerTcpServer...");
        isRunning.set(false);
        
        try {
            logger.info("Cerrando {} conexiones entrantes...", incomingPeerSockets.size());
            for (String peerId : incomingPeerSockets.keySet()) {
                disconnectIncomingPeer(peerId);
            }

            if (peerServerSocket != null && !peerServerSocket.isClosed()) {
                peerServerSocket.close();
                logger.info("Socket servidor P2P cerrado");
            }

            if (acceptorThread != null && acceptorThread.isAlive()) {
                acceptorThread.join(3000);
                if (acceptorThread.isAlive()) {
                    logger.warn("Hilo acceptor no terminó en tiempo esperado");
                }
            }
            
        } catch (IOException e) {
            logger.error("Error cerrando PeerTcpServer: {}", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("Interrumpido esperando cierre de PeerTcpServer: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
        
        logger.info("PeerTcpServer detenido completamente");
    }

    public boolean isRunning() {
        return isRunning.get();
    }
    
    public int getPeerPort() {
        return peerPort;
    }

    private String getLocalIp() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("No se pudo obtener IP local, usando 127.0.0.1: {}", e.getMessage());
            return "127.0.0.1";
        }
    }
}