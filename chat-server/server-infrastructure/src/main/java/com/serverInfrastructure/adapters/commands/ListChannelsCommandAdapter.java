package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.Channel;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;

import java.util.List;
import java.util.stream.Collectors;

public class ListChannelsCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;

    public ListChannelsCommandAdapter(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public String getCommandName() {
        return "LIST_CHANNELS";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        var aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String userId = aum.getUserIdFromConnection(connectionContext.getId());
        
        if (userId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }
        
        List<Channel> channels = channelRepository.findAllForUser(userId);
        
        String payload = channels.stream()
                .map(c -> c.getId() + "," + c.getName() + "," + c.getVisibility())
                .collect(Collectors.joining(";"));
        return parser.encode("OK", payload);
    }
}


