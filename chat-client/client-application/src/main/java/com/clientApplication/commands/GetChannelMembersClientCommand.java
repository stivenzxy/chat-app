package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.Arrays;
import java.util.List;

public class GetChannelMembersClientCommand implements ClientCommand<Integer, List<String>> {

    private final ServerGatewayPort gateway;

    public GetChannelMembersClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<String> execute(Integer channelId) {
        try {
            List<String> responseParts = gateway.sendAndReceive(
                "GET_CHANNEL_MEMBERS",
                String.valueOf(channelId)
            );
            
            if (!responseParts.isEmpty() && "OK".equals(responseParts.get(0))) {
                String membersPayload = responseParts.size() > 1 ? responseParts.get(1) : "";
                if (membersPayload.isEmpty()) {
                    return List.of();
                }
                return Arrays.asList(membersPayload.split(","));
            } else {
                return List.of();
            }
        } catch (Exception e) {
            return List.of();
        }
    }

    public static class Request {
        private final Integer channelId;

        public Request(Integer channelId) {
            this.channelId = channelId;
        }

        public Integer getChannelId() {
            return channelId;
        }
    }
}
