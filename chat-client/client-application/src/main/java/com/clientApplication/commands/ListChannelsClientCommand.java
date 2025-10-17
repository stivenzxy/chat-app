package com.clientApplication.commands;

import com.chatCommon.dto.ChannelDTO;
import com.chatCommon.dto.ChannelVisibility;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ListChannelsClientCommand implements ClientCommand<Void, List<ChannelDTO>> {
    private final ServerGatewayPort gateway;

    public ListChannelsClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<ChannelDTO> execute(Void request) {
        try {
            List<String> parts = gateway.sendAndReceive("LIST_CHANNELS");
            if (parts.isEmpty() || !"OK".equalsIgnoreCase(parts.getFirst())) return List.of();
            if (parts.size() < 2) return List.of();
            String payload = parts.get(1);
            if (payload == null || payload.isEmpty()) return List.of();
            String[] items = payload.split(";");
            List<ChannelDTO> channels = new ArrayList<>();
            for (String item : items) {
                String[] fields = item.split(",");
                if (fields.length >= 3) {
                    Integer id = Integer.valueOf(fields[0]);
                    String name = fields[1];
                    ChannelVisibility vis = ChannelVisibility.valueOf(fields[2]);
                    channels.add(new ChannelDTO(id, name, null, vis, LocalDateTime.now()));
                }
            }
            return channels;
        } catch (Exception e) {
            return List.of();
        }
    }
}


