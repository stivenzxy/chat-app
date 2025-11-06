package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SendPrivateMessageCommandAdapter implements ProtocolCommandAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SendPrivateMessageCommandAdapter.class);
    
    private final TcpServer server;
    private final MessageDAO messageDAO = new MessageDAO();
    
    // Callback para enrutamiento P2P (inyectado externamente)
    private java.util.function.BiFunction<String, String, Boolean> peerRoutingCallback;

    public SendPrivateMessageCommandAdapter(TcpServer server) {
        this.server = server;
    }

    @Override
    public String getCommandName() {
        return "SEND_PRIVATE_MESSAGE";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 3) {
            return parser.encode("ERROR", "Argumentos insuficientes para enviar mensaje.");
        }

        // SEND_PRIVATE_MESSAGE|destinatario_username|contenido_mensaje
        String senderConnectionId = connectionContext.getId();

        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String senderUsername = aum.getAllUserSessions().entrySet().stream()
            .filter(entry -> entry.getValue().stream()
                .anyMatch(user -> user.getId().equals(senderConnectionId)))
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
            
        if (senderUsername == null) {
            return parser.encode("ERROR", "Remitente no válido");
        }
        
        String recipientUsername = parts.get(1);
        String content = parts.get(2);
        
        String senderUserId = aum.getUserIdFromConnection(senderConnectionId);
        
        String forwardMessage = parser.encode("RECEIVE_PRIVATE_MESSAGE", senderUsername, content);

        String senderInfo = getSenderInfo(connectionContext);

        // Intentar entrega local primero
        boolean delivered = server.sendMessageToUser(recipientUsername, forwardMessage, senderInfo);

        // Si no está local, intentar enrutar a través de peer
        if (!delivered && peerRoutingCallback != null) {
            logger.info("Usuario {} no está local, intentando enrutamiento P2P...", recipientUsername);
            
            // Extraer el username real si viene con prefijo "Servidor X - username"
            String actualUsername = recipientUsername;
            if (recipientUsername.contains(" - ")) {
                // Formato: "Servidor 127.0.0.1 - jesus.perez" -> "jesus.perez"
                actualUsername = recipientUsername.substring(recipientUsername.lastIndexOf(" - ") + 3);
                logger.info("Username extraído del prefijo: {} -> {}", recipientUsername, actualUsername);
            }
            
            // Construir mensaje de enrutamiento: P2P_ROUTE_PRIVATE|senderUsername|recipientUsername|content
            String routeMessage = "P2P_ROUTE_PRIVATE|" + senderUsername + "|" + actualUsername + "|" + content;
            delivered = peerRoutingCallback.apply(actualUsername, routeMessage);
            
            if (delivered) {
                logger.info("Mensaje enrutado exitosamente a usuario remoto {}", actualUsername);
            } else {
                logger.warn("No se pudo enrutar mensaje a usuario remoto {}", actualUsername);
            }
        }

        if (delivered) {
            String recipientConnectionId = aum.getUserSessions(recipientUsername).stream()
                .findFirst()
                .map(user -> user.getId())
                .orElse(null);
            
            String recipientUserId = null;
            if (recipientConnectionId != null) {
                recipientUserId = aum.getUserIdFromConnection(recipientConnectionId);
            }
            
            if (!content.startsWith("[TRANSCRIPCIÓN]") && senderUserId != null && recipientUserId != null) {
                try {
                    messageDAO.savePrivateTextMessage(senderUserId, recipientUserId, content);
                } catch (Exception e) {
                    org.slf4j.LoggerFactory.getLogger(SendPrivateMessageCommandAdapter.class)
                        .error("Error al guardar mensaje de texto: {}", e.getMessage());
                }
            }
            
            String echoMessage = parser.encode("ECHO_SENT_MESSAGE", recipientUsername, content);
            server.sendMessageToUserExceptSession(senderUsername, echoMessage, senderConnectionId, null);
            
            return parser.encode("OK", "Mensaje enviado.");
        } else {
            return parser.encode("ERROR", "El usuario no está conectado o no existe.");
        }
    }
    
    private String getSenderInfo(ClientConnection connection) {
        try {
            String ip = connection.getSocket().getInetAddress().getHostAddress();
            return connection.getId() + " | IP: " + ip;
        } catch (Exception e) {
            return connection.getId();
        }
    }
    
    /**
     * Establece el callback para enrutamiento P2P.
     * Este método será llamado por InfrastructureFactory durante la configuración.
     */
    public void setPeerRoutingCallback(java.util.function.BiFunction<String, String, Boolean> callback) {
        this.peerRoutingCallback = callback;
        logger.info("Callback de enrutamiento P2P configurado en SendPrivateMessageCommandAdapter");
    }
}