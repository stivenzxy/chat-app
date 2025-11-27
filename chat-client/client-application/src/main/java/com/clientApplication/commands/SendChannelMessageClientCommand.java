package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.List;

public class SendChannelMessageClientCommand
        implements ClientCommand<SendChannelMessageClientCommand.Request, Boolean> {
    public static class Request {
        public final String channelId;
        public final String content;

        public Request(String channelId, String content) {
            this.channelId = channelId;
            this.content = content;
        }
    }

    private final ServerGatewayPort gateway;

    public SendChannelMessageClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Boolean execute(Request request) {
        try {
            List<String> parts = gateway.sendAndReceive("SEND_CHANNEL_MESSAGE", request.channelId, request.content);
            return !parts.isEmpty() && "OK".equalsIgnoreCase(parts.getFirst());
        } catch (Exception e) {
            return false;
        }
    }
}
