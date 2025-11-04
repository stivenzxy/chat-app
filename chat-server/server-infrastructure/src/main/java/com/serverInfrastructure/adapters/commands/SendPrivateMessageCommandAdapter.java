package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer; // Necesitaremos una referencia al servidor

import java.util.List;
import java.util.Map;

public class SendPrivateMessageCommandAdapter implements ProtocolCommandAdapter {

    private final TcpServer server;

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
        // Buscar el username en TODAS las sesiones activas
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

        String forwardMessage = parser.encode("RECEIVE_PRIVATE_MESSAGE", senderUsername, content);

        String senderInfo = getSenderInfo(connectionContext);

        boolean delivered = server.sendMessageToUser(recipientUsername, forwardMessage, senderInfo);

        if (delivered) {
            // NUEVO: Sincronizar con las otras sesiones del remitente
            // Usar un formato especial: ECHO_SENT_MESSAGE para indicar que es un mensaje enviado por ellos
            // Formato: ECHO_SENT_MESSAGE|destinatario|contenido
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