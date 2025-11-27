package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.List;

public class RespondInviteClientCommand implements ClientCommand<RespondInviteClientCommand.Request, Boolean> {
    public static class Request {
        public final String inviteId;
        public final String status;
        public final String channelId;

        public Request(String inviteId, String status, String channelId) {
            this.inviteId = inviteId;
            this.status = status;
            this.channelId = channelId;
        }
    }

    private final ServerGatewayPort gateway;

    public RespondInviteClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Boolean execute(Request request) {
        try {
            List<String> parts = gateway.sendAndReceive("RESPOND_INVITE",
                    request.inviteId, request.status, request.channelId);
            return !parts.isEmpty() && "OK".equalsIgnoreCase(parts.getFirst());
        } catch (Exception e) {
            return false;
        }
    }
}
