package com.clientApplication.commands;

import com.chatCommon.dto.MessageDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;
import java.util.Base64;
import java.util.List;

public class SendPrivateAudioClientCommand implements ClientCommand<MessageDTO, Boolean> {

    private final ServerGatewayPort gateway;

    public SendPrivateAudioClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Boolean execute(MessageDTO request) {
        if (request.getAudioContent() == null) {
            return false;
        }

        try {
            // Codificar los bytes del audio a Base64
            String audioBase64 = Base64.getEncoder().encodeToString(request.getAudioContent());

            List<String> responseParts = gateway.sendAndReceive(
                    "SEND_PRIVATE_AUDIO",
                    request.getRecipientId(),
                    audioBase64
            );
            return !responseParts.isEmpty() && "OK".equalsIgnoreCase(responseParts.get(0));
        } catch (Exception e) {
            return false;
        }
    }
}