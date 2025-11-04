package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.observers.ActiveUserManager;

import java.util.List;

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
        String senderUserId = connectionContext.getId();

        ActiveUserManager aum = ActiveUserManager.getInstance();
        String senderUsername = aum.getActiveUsers().entrySet().stream()
            .filter(entry -> entry.getValue().getId().equals(senderUserId))
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
            
        if (senderUsername == null) {
            return parser.encode("ERROR", "Remitente no válido");
        }
        
        String recipientUsername = parts.get(1);
        String content = parts.get(2);
        
        String recipientUserId = aum.getActiveUsers().entrySet().stream()
            .filter(entry -> entry.getKey().equals(recipientUsername))
            .map(entry -> entry.getValue().getId())
            .findFirst()
            .orElse(null);
        
        if (recipientUserId == null) {
            return parser.encode("ERROR", "El usuario no está conectado o no existe.");
        }

        if (!content.startsWith("[TRANSCRIPCIÓN]")) {
            try {
                messageDAO.savePrivateTextMessage(senderUserId, recipientUserId, content);
            } catch (Exception e) {
                org.slf4j.LoggerFactory.getLogger(SendPrivateMessageCommandAdapter.class)
                    .error("Error al guardar mensaje de texto: {}", e.getMessage());
            }
        }

        String forwardMessage = parser.encode("RECEIVE_PRIVATE_MESSAGE", senderUsername, content);

        String senderInfo = getSenderInfo(connectionContext);

        boolean delivered = server.sendMessageToUser(recipientUsername, forwardMessage, senderInfo);

        if (delivered) {
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