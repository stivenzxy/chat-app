package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.List;

public class InviteToChannelClientCommand implements ClientCommand<InviteToChannelClientCommand.Request, Boolean> {
    public static class Request {
        public final int channelId;
        public final String invitedUserId;
        public Request(int channelId, String invitedUserId) {
            this.channelId = channelId;
            this.invitedUserId = invitedUserId;
        }
    }

    private final ServerGatewayPort gateway;

    public InviteToChannelClientCommand(ServerGatewayPort gateway) { this.gateway = gateway; }

    @Override
    public Boolean execute(Request request) {
        try {
            List<String> parts = gateway.sendAndReceive("INVITE_TO_CHANNEL", String.valueOf(request.channelId), request.invitedUserId);
            return !parts.isEmpty() && "OK".equalsIgnoreCase(parts.getFirst());
        } catch (Exception e) {
            return false;
        }
    }
}


