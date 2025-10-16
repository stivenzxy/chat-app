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
                System.err.println("Error obteniendo miembros del canal: " + 
                    (responseParts.size() > 1 ? responseParts.get(1) : "Respuesta inválida"));
                return List.of();
            }
        } catch (Exception e) {
            System.err.println("Error ejecutando comando GET_CHANNEL_MEMBERS: " + e.getMessage());
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
