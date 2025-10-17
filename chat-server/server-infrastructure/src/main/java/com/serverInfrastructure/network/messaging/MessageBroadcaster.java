package com.serverInfrastructure.network.messaging;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

public class MessageBroadcaster {
    private static final Logger logger = LoggerFactory.getLogger(MessageBroadcaster.class);
    
    private final ConnectionPool connectionPool;
    private final ProtocolParser protocolParser;

    public MessageBroadcaster(ConnectionPool connectionPool, ProtocolParser protocolParser) {
        this.connectionPool = connectionPool;
        this.protocolParser = protocolParser;
    }

    public boolean sendMessageToUser(String username, String message, String senderInfo) {
        ClientConnection connection = connectionPool.findConnectionByUsername(username);
        if (connection != null) {
            try {
                if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                    PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                    out.println(message);

                    if (senderInfo != null) {
                        String messageContent = extractMessageContent(message);
                        logger.info("Cliente [{}] envió \"{}\" a cliente [{}]",
                                senderInfo, messageContent, username);
                    } else {
                        String messageContent = extractMessageContent(message);
                        logger.info("Mensaje directo enviado a [{}]: {}", username, messageContent);
                    }
                    return true;
                }
            } catch (IOException e) {
                logger.warn("[{}] Error al enviar mensaje directo: {}", username, e.getMessage());
            }
        }
        return false;
    }

    public void broadcastMessage(String message, String excludeUsername) {
        String messageContent = extractMessageContent(message);
        if (excludeUsername != null) {
            logger.info("Broadcasting: {} (excluyendo a: {})", messageContent, excludeUsername);
        } else {
            logger.info("Broadcasting: {}", messageContent);
        }
        
        for (ClientConnection connection : connectionPool.getInUseConnections()) {
            if (connection.getId() != null && !connection.getId().equals(excludeUsername)) {
                try {
                    if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                        PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                        out.println(message);
                    }
                } catch (IOException e) {
                    logger.warn("[{}] Error al hacer broadcast: {}", connection.getId(), e.getMessage());
                }
            }
        }
    }

    public void notifyClientDisconnection(String clientId) {
        ClientConnection connection = connectionPool.findConnectionById(clientId);
        if (connection != null) {
            try {
                if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                    PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                    String disconnectMessage = protocolParser.encode("DISCONNECT", "Desconectado por el servidor");
                    out.println(disconnectMessage);
                    logger.info("[{}] Notificación de desconexión enviada al cliente", clientId);

                    Thread.sleep(100);
                }
            } catch (Exception e) {
                logger.warn("[{}] Error al enviar notificación de desconexión: {}", clientId, e.getMessage());
            }

            connectionPool.forceDisconnect(connection);
        } else {
            logger.warn("No se encontró cliente con ID: {}", clientId);
        }
    }

    private String extractMessageContent(String protocolMessage) {
        try {
            List<String> parts = protocolParser.decode(protocolMessage);
            if (parts.size() >= 2) {
                String command = parts.get(0);
                switch (command) {
                    case "RECEIVE_PRIVATE_MESSAGE":
                        if (parts.size() >= 3) {
                            return parts.get(2);
                        }
                        break;
                    case "RECEIVE_PRIVATE_AUDIO":
                        return "[Mensaje de audio]";
                    case "USER_CONNECTED":
                        if (parts.size() >= 3) {
                            String username = parts.get(2);
                            boolean hasPhoto = parts.size() >= 4 && !parts.get(3).isEmpty();
                            return hasPhoto ? "Usuario conectado: " + username + " [con foto de perfil]" 
                                           : "Usuario conectado: " + username;
                        }
                        break;
                    case "USER_DISCONNECTED":
                        if (parts.size() >= 3) {
                            return "Usuario desconectado: " + parts.get(2);
                        }
                        break;
                    case "SERVER_BROADCAST":
                        if (parts.size() >= 2) {
                            return "Broadcast del servidor: " + parts.get(1);
                        }
                        break;
                    case "RECEIVE_CHANNEL_MESSAGE":
                        if (parts.size() >= 4) {
                            return "Mensaje de canal [" + parts.get(1) + "] de " + parts.get(2) + ": " + parts.get(3);
                        }
                        break;
                    case "RECEIVE_CHANNEL_AUDIO":
                        if (parts.size() >= 3) {
                            return "Audio de canal [" + parts.get(1) + "] de " + parts.get(2) + ": [Mensaje de audio]";
                        }
                        break;
                }
            }
            return parts.isEmpty() ? protocolMessage : "Comando: " + parts.get(0);
        } catch (Exception e) {
            return "Mensaje de protocolo";
        }
    }
}