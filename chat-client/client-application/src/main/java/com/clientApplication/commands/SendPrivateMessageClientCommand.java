package com.clientApplication.commands;

import com.chatCommon.dto.MessageDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.List;

public class SendPrivateMessageClientCommand implements ClientCommand<MessageDTO, Boolean> {

    private final ServerGatewayPort gateway;

    public SendPrivateMessageClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Boolean execute(MessageDTO request) {
        try {
            List<String> responseParts = gateway.sendAndReceive(
                    "SEND_PRIVATE_MESSAGE",
                    request.getRecipientId(),
                    request.getTextContent() // CORRECTO
            );
            return !responseParts.isEmpty() && "OK".equalsIgnoreCase(responseParts.get(0));
        } catch (Exception e) {
            // ...
            return false;
        }
    }
}