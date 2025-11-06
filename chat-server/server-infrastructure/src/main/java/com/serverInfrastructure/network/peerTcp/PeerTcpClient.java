package com.serverInfrastructure.network.peerTcp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
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
    
    public PeerTcpClient() {
        logger.debug("PeerTcpClient creado - Listo para conectar");
    }

    public void setOnConnectionRejected(Runnable callback) {
        this.onConnectionRejected = callback;
    }
    public void setOnUserSyncReceived(Consumer<String> callback) {
        this.onUserSyncReceived = callback;
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

    private void startListening() {
        listenerThread = new Thread(() -> {
            logger.info("Iniciando escucha de mensajes desde peer {}", peerId);
            
            while (isConnected.get() && !Thread.currentThread().isInterrupted()) {
                try {
                    String message = input.readLine();
                    
                    if (message == null) {
                        logger.info("Peer {} cerró la conexión", peerId);
                        break;
                    }

                    logger.debug("Mensaje recibido de {}: {}", peerId, message);
                    handleIncomingMessage(message);
                    
                } catch (SocketException e) {
                    if (isConnected.get() && !Thread.currentThread().isInterrupted()) {
                        logger.warn("Conexión con peer {} perdida: {}", peerId, e.getMessage());
                    }
                    break;
                } catch (IOException e) {
                    if (isConnected.get() && !Thread.currentThread().isInterrupted()) {
                        logger.error("Error leyendo mensaje de peer {}: {}", peerId, e.getMessage());
                    }
                    break;
                } catch (Exception e) {
                    logger.error("Error crítico procesando mensaje de peer {}: {}", peerId, e.getMessage(), e);
                }
            }
            
        }, "PeerListener-" + peerId);
        
        listenerThread.start();
    }

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

        String handshakeMessage = String.format("P2P_SERVER_HANDSHAKE|version=1.0|type=PEER_SERVER|myId=%s:%d|protocol=CHAT_P2P|users=[]|channels=[]", 
            getLocalIp(), getLocalPort());
        
        logger.info("Enviando handshake P2P a peer {}", peerId);
        sendMessage(handshakeMessage);
    }

    public void disconnect() {
        if (!isConnected.get()) {
            logger.debug("PeerTcpClient ya estaba desconectado");
            return;
        }
        
        logger.info("Desconectando de peer {}", peerId);
        isConnected.set(false);

        // Enviar mensaje de desconexión si es posible
        try {
            if (output != null) {
                output.println("DISCONNECT");
            }
        } catch (Exception e) {
            logger.debug("No se pudo enviar mensaje de desconexión: {}", e.getMessage());
        }
        
        // Cerrar todos los recursos
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null && !socket.isClosed()) socket.close();
            
            // Interrumpir hilo listener
            if (listenerThread != null && listenerThread.isAlive()) {
                listenerThread.interrupt();
            }
            
            logger.info("Desconectado de peer {}", peerId);
        } catch (Exception e) {
            logger.error("Error limpiando recursos al desconectar: {}", e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return isConnected.get() && socket != null && !socket.isClosed();
    }

    private String getLocalIp() {
        return "127.0.0.1"; 
    }

    private int getLocalPort() {
        return 12346; 
    }
}