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
                        logger.info("Mensaje directo enviado a [{}]: {}", username, message);
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
        logger.info("Broadcasting: {}", message);
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
            if (parts.size() >= 3) {
                String command = parts.get(0);
                if ("RECEIVE_PRIVATE_MESSAGE".equals(command)) {
                    return parts.get(2);
                } else if ("RECEIVE_PRIVATE_AUDIO".equals(command)) {
                    return "[Audio message]";
                }
            }
            return protocolMessage;
        } catch (Exception e) {
            return protocolMessage;
        }
    }
}