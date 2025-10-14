package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;

import java.util.List;

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

        // Formato: SEND_PRIVATE_AUDIO|destinatario_id|audio_en_base64
        String senderId = connectionContext.getId();
        String recipientId = parts.get(1);
        String audioBase64 = parts.get(2);

        // Mensaje que se reenviará al destinatario
        String forwardMessage = parser.encode("RECEIVE_PRIVATE_AUDIO", senderId, audioBase64);

        boolean delivered = server.sendMessageToUser(recipientId, forwardMessage);

        if (delivered) {
            return parser.encode("OK", "Audio enviado.");
        } else {
            return parser.encode("ERROR", "El usuario no está conectado o no existe.");
        }
    }
}