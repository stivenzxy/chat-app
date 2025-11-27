package com.clientApplication.commands;

import com.chatCommon.dto.ChannelVisibility;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.List;

public class CreateChannelClientCommand implements ClientCommand<CreateChannelClientCommand.Request, String> {

    public static class Request {
        public final String name;
        public final ChannelVisibility visibility;

        public Request(String name, ChannelVisibility visibility) {
            this.name = name;
            this.visibility = visibility;
        }
    }

    private final ServerGatewayPort gateway;

    public CreateChannelClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public String execute(Request request) {
        try {
            List<String> parts = gateway.sendAndReceive(
                    "CREATE_CHANNEL",
                    request.name,
                    request.visibility.name());
            if (!parts.isEmpty() && "OK".equalsIgnoreCase(parts.getFirst()) && parts.size() > 1) {
                return parts.get(1);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
