package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.List;

public class InviteToChannelClientCommand implements ClientCommand<InviteToChannelClientCommand.Request, String> {
    public static class Request {
        public final String channelId;
        public final String invitedUserId;

        public Request(String channelId, String invitedUserId) {
            this.channelId = channelId;
            this.invitedUserId = invitedUserId;
        }
    }

    private final ServerGatewayPort gateway;

    public InviteToChannelClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public String execute(Request request) {
        try {
            List<String> parts = gateway.sendAndReceive("INVITE_TO_CHANNEL", request.channelId, request.invitedUserId);
            if (parts.isEmpty()) {
                return "Error de comunicación con el servidor.";
            }

            if ("OK".equalsIgnoreCase(parts.getFirst())) {
                return "OK";
            } else {
                return parts.size() > 1 ? parts.get(1) : "Error desconocido del servidor.";
            }
        } catch (Exception e) {
            return "Excepción de comunicación: " + e.getMessage();
        }
    }
}