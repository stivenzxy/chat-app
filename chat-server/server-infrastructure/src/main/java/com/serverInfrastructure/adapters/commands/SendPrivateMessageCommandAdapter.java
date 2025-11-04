package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.observers.ActiveUserManager;

import java.util.List;
import java.util.Map;

public class SendPrivateMessageCommandAdapter implements ProtocolCommandAdapter {

    private final TcpServer server;
    private final MessageDAO messageDAO = new MessageDAO();

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

        boolean delivered = server.sendMessageToUser(recipientUsername, forwardMessage, senderInfo);

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
}