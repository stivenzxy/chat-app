package com.serverInfrastructure.network.peerTcp;

import com.chatCommon.utils.AppProperties;
import com.serverInfrastructure.adapters.peer.managers.PeerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class PeerTcpClient {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerTcpClient.class);
    
    private Socket socket;
    private BufferedReader input;
    private PrintWriter output;
    private Thread listenerThread;
    private final AtomicBoolean isConnected = new AtomicBoolean(false);
    
    private String peerIp;
    private int peerPort;
    private String peerId; 

    private Runnable onConnectionRejected;
    private java.util.function.Consumer<String> onUserSyncReceived;
    private java.util.function.BiConsumer<String, String> onPrivateMessageReceived;
    private java.util.function.Consumer<String> onPeerListReceived;
    
    private Runnable onDisconnected;

    public PeerTcpClient() {
        logger.debug("PeerTcpClient creado - Listo para conectar");
    }

    public void setOnConnectionRejected(Runnable callback) {
        this.onConnectionRejected = callback;
    }
    public void setOnUserSyncReceived(Consumer<String> callback) {
        this.onUserSyncReceived = callback;
    }

    public void setOnPeerListReceived(Consumer<String> callback) {
        this.onPeerListReceived = callback;
    }

    public void setOnPrivateMessageReceived(BiConsumer<String, String> callback) {
        this.onPrivateMessageReceived = callback;
    }
    
    public boolean connectToPeer(String ip, int port) {
        if (isConnected.get()) {
            logger.warn("Ya existe una conexión activa a {}:{}", this.peerIp, this.peerPort);
            return false;
        }
        
        this.peerIp = ip;
        this.peerPort = port;
        this.peerId = ip + ":" + port;
        
        try {
            logger.info("Iniciando conexión a peer {}:{}", ip, port);
            socket = new Socket(ip, port);
            

            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            
            isConnected.set(true);
            
            logger.info("Conectado exitosamente a peer {}", peerId);
            
            startListening();
            
            return true;
            
        } catch (IOException e) {
            logger.error("Error conectando a peer {}:{} - {}", ip, port, e.getMessage());
            isConnected.set(false);
            try {
                if (input != null) input.close();
                if (output != null) output.close();
                if (socket != null && !socket.isClosed()) socket.close();
            } catch (IOException ex) {
                logger.debug("Error limpiando recursos: {}", ex.getMessage());
            }
            return false;
        }
    }

     public void setOnDisconnected(Runnable onDisconnected) {
        this.onDisconnected = onDisconnected;
    }

    private void startListening() {
        listenerThread = new Thread(() -> {
            logger.info("Iniciando escucha de mensajes desde peer {}", peerId);
            
            try {
                while (isConnected.get() && !Thread.currentThread().isInterrupted()) {
                    String message = input.readLine();
                    if (message == null) {
                        logger.info("Peer {} cerró la conexión (end of stream)", peerId);
                        break; // Salir del bucle si se cierra la conexión
                    }
                    handleIncomingMessage(message);
                }
            } catch (IOException e) {
                if (isConnected.get()) { // Evitar logs de error si nos desconectamos a propósito
                    logger.warn("Conexión con peer {} perdida: {}", peerId, e.getMessage());
                }
            } finally {
                // ======== MODIFICACIÓN CLAVE DENTRO DE finally ========
                // Asegurarse de que la desconexión se maneje y notifique solo una vez.
                if (isConnected.getAndSet(false)) { // Atomically set to false
                    logger.info("El hilo de escucha para {} ha terminado. Notificando desconexión.", peerId);
                    if (onDisconnected != null) {
                        onDisconnected.run();
                    }
                }
                // Limpieza final de recursos
                cleanupResources();
            }
        }, "PeerListener-" + peerId);
        
        listenerThread.start();
    }

    private boolean hasReceivedInitialSync = false;

    private void handleIncomingMessage(String message) {
        logger.info("Procesando mensaje de {}: {}", peerId, message);
        
        if (message.startsWith("P2P_SERVER_HANDSHAKE_ACK")) {
            logger.info("Recibido P2P_SERVER_HANDSHAKE_ACK de {}", peerId);
            
        } else if (message.startsWith("P2P_CONNECTION_REJECTED")) {
            logger.error("Conexión P2P rechazada por {}: {}", peerId, message);

            if (onConnectionRejected != null) {
                onConnectionRejected.run();
            }

            disconnect();
        } else if (message.startsWith("P2P_USER_SYNC")) {
            logger.info("(CLIENT) Recibida sincronización de usuarios desde {}: {}", peerId, message);
            if (onUserSyncReceived != null) {
                onUserSyncReceived.accept(message);
            }
            // Si es la primera sincronización completa recibida, responder con nuestros usuarios
            if (!hasReceivedInitialSync && message.contains("action=SYNC_ALL")) {
                hasReceivedInitialSync = true;
                // La respuesta se envía automáticamente en el callback onConnectionSuccess del PeerConnectionManager
                logger.info("Primera sincronización completa recibida desde {}, se enviará respuesta", peerId);
            }

        } else if (message.startsWith("P2P_PEER_LIST")) {
            logger.info("(CLIENT) Lista de peers recibida desde {}: {}", peerId, message);
            if (onPeerListReceived != null) {
                onPeerListReceived.accept(message);
            }
        } else if (message.startsWith("P2P_ROUTE_PRIVATE_AUDIO")) {
            logger.info("(CLIENT) Audio privado enrutado recibido desde {}: {}", peerId, message);
            // Notificar via callback para que el adapter lo entregue al cliente local
            if (onPrivateMessageReceived != null) {
                onPrivateMessageReceived.accept(peerId, message);
            }
            
        } else if (message.startsWith("P2P_ROUTE_PRIVATE")) {
            logger.info("(CLIENT) Mensaje privado enrutado recibido desde {}: {}", peerId, message);
            // Notificar via callback para que el adapter lo entregue al cliente local
            if (onPrivateMessageReceived != null) {
                onPrivateMessageReceived.accept(peerId, message);
            }
            
        } else if (message.startsWith("P2P_CHANNEL_INVITE")) {
            logger.info("(CLIENT) Invitación de canal enrutada recibida desde {}: {}", peerId, message);
            if (onPrivateMessageReceived != null) {
                onPrivateMessageReceived.accept(peerId, message);
            }
            
        } else if (message.startsWith("P2P_CHANNEL_MESSAGE")) {
            logger.info("(CLIENT) Mensaje de canal enrutado recibido desde {}: {}", peerId, message);
            if (onPrivateMessageReceived != null) {
                onPrivateMessageReceived.accept(peerId, message);
            }
            
        } else if (message.startsWith("USER_JOINED")) {
            logger.info("Usuario se unió en peer {}", peerId);
            
        } else if (message.startsWith("ROUTE_MESSAGE")) {
            logger.info("Mensaje enrutado desde peer {}", peerId);
            
        } else {
            logger.debug("Mensaje genérico de peer {}: {}", peerId, message);
        }
    }

    public boolean sendMessage(String message) {
        if (!isConnected.get() || output == null) {
            logger.error("No hay conexión activa para enviar mensaje a {}", peerId);
            return false;
        }
        
        try {
            output.println(message);
            
            if (output.checkError()) {
                logger.error("Error enviando mensaje a peer {}", peerId);
                return false;
            }
            
            logger.debug("Mensaje enviado a {}: {}", peerId, message);
            return true;
            
        } catch (Exception e) {
            logger.error("Error enviando mensaje a peer {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    public void sendHandshake() {
        if (!isConnected.get()) {
            logger.error("No se puede enviar evento de conexión inicial - no hay conexión");
            return;
        }
        // Announce configured PEER_SERVER_PORT (the server's listening P2P port) instead of the ephemeral TCP local port
        String handshakeMessage = String.format("P2P_SERVER_HANDSHAKE|version=1.0|type=PEER_SERVER|myId=%s:%d|protocol=CHAT_P2P|users=[]|channels=[]",
            getLocalIp(), getConfiguredPeerServerPort());

        logger.info("Enviando handshake P2P a peer {} (advertised myId={}:{})", peerId, getLocalIp(), getConfiguredPeerServerPort());
        sendMessage(handshakeMessage);
        // También enviar lista local de peers conocida
        try {
            sendPeerList();
        } catch (Exception e) {
            logger.debug("No se pudo enviar peer list tras handshake: {}", e.getMessage());
        }
    }

    public void sendPeerList() {
        // Construir mensaje con peers conocidos (si existe registry)
        try {
            PeerRegistry registry = PeerRegistry.getInstance();
            java.util.Set<String> peers = registry.getValidPeers();
            if (peers.isEmpty()) return;
            String payload = String.join(",", peers);
            String message = "P2P_PEER_LIST|peers=" + payload;
            sendMessage(message);
            logger.debug("Enviado P2P_PEER_LIST a {}: {} peers sent", peerId, peers.size());
        } catch (Exception e) {
            logger.debug("Error construyendo/enviando peer list: {}", e.getMessage());
        }
    }

    public void disconnect() {
        // ======== MODIFICACIÓN EN disconnect() ========
        // Usa getAndSet para asegurar que el bloque se ejecute solo una vez.
        if (!isConnected.getAndSet(false)) {
            logger.debug("PeerTcpClient a {} ya estaba desconectado.", peerId);
            return;
        }
        
        logger.info("Iniciando desconexión de peer {}", peerId);

        // Notificar inmediatamente para una respuesta de UI más rápida
        if (onDisconnected != null) {
            onDisconnected.run();
        }
        
        cleanupResources();
        logger.info("Desconectado de peer {}", peerId);
    }
    
    // ======== AÑADIR ESTE NUEVO MÉTODO PRIVADO ========
    private void cleanupResources() {
        try {
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error("Error limpiando recursos para peer {}: {}", peerId, e.getMessage());
        } finally {
            input = null;
            output = null;
            socket = null;
        }
    }
    
    public boolean isConnected() {
        return isConnected.get() && socket != null && !socket.isClosed();
    }

    private String getLocalIp() {
        try {
            if (socket != null && socket.getLocalAddress() != null) {
                return socket.getLocalAddress().getHostAddress();
            }
        } catch (Exception ignored) {
        }
        return "127.0.0.1";
    }

    private int getLocalPort() {
        try {
            if (socket != null) {
                return socket.getLocalPort();
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    private int getConfiguredPeerServerPort() {
        try {
            AppProperties props = new AppProperties("server-configuration");
            int p = props.getInt("PEER_SERVER_PORT");
            if (p > 0) return p;
        } catch (Exception e) {
            logger.debug("No se pudo leer PEER_SERVER_PORT desde configuración: {}", e.getMessage());
        }
        return getLocalPort();
    }
}