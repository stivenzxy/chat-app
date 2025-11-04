package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;

import java.util.List;
import java.util.Map;

public class SendPrivateAudioCommandAdapter implements ProtocolCommandAdapter {

    private final TcpServer server;

    public SendPrivateAudioCommandAdapter(TcpServer server) {
        this.server = server;
    }

    @Override
    public String getCommandName() {
        return "SEND_PRIVATE_AUDIO";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 3) {
            return parser.encode("ERROR", "Argumentos insuficientes para enviar audio.");
        }

        // Formato: SEND_PRIVATE_AUDIO|destinatario_username|audio_en_base64
        String senderConnectionId = connectionContext.getId();
        
        // Obtener username del usuario que envía el audio
        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        // Buscar el username en TODAS las sesiones activas
        String senderUsername = aum.getAllUserSessions().entrySet().stream()
            .filter(entry -> entry.getValue().stream()
                .anyMatch(user -> user.getId().equals(senderConnectionId)))
            .map(java.util.Map.Entry::getKey)
            .findFirst()
            .orElse(null);
            
        if (senderUsername == null) {
            return parser.encode("ERROR", "Remitente no válido");
        }
        
        String recipientUsername = parts.get(1);
        String audioBase64 = parts.get(2);

        String forwardMessage = parser.encode("RECEIVE_PRIVATE_AUDIO", senderUsername, audioBase64);

        String senderInfo = getSenderInfo(connectionContext) + " [AUDIO]";

        boolean delivered = server.sendMessageToUser(recipientUsername, forwardMessage, senderInfo);

        if (delivered) {
            // NUEVO: Sincronizar con las otras sesiones del remitente
            // Usar formato especial para indicar que es un audio enviado por ellos
            String echoAudio = parser.encode("ECHO_SENT_AUDIO", recipientUsername, audioBase64);
            server.sendMessageToUserExceptSession(senderUsername, echoAudio, senderConnectionId, null);
            
            return parser.encode("OK", "Audio enviado.");
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