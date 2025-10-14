package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer; // Necesitaremos una referencia al servidor

import java.util.List;

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

        // Formato: SEND_PRIVATE_MESSAGE|destinatario_id|contenido_mensaje
        String senderId = connectionContext.getId();
        String recipientId = parts.get(1);
        String content = parts.get(2);

        // Mensaje que se enviará al destinatario
        String forwardMessage = parser.encode("RECEIVE_PRIVATE_MESSAGE", senderId, content);

        // Pedir al servidor que envíe el mensaje
        boolean delivered = server.sendMessageToUser(recipientId, forwardMessage);

        if (delivered) {
            // Confirmación para el remitente
            return parser.encode("OK", "Mensaje enviado.");
        } else {
            return parser.encode("ERROR", "El usuario no está conectado o no existe.");
        }
    }
}