package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.Channel;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;

import java.time.LocalDateTime;
import java.util.List;

public class CreateChannelCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;

    public CreateChannelCommandAdapter(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public String getCommandName() {
        return "CREATE_CHANNEL";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        // CREATE_CHANNEL|name|visibility(PUBLIC|PRIVATE)
        if (parts.size() < 3) return parser.encode("ERROR", "Argumentos insuficientes");
        
        // Obtener el userId REAL del usuario desde el connectionId
        String connectionId = connectionContext.getId();
        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        
        String ownerUserId = aum.getUserIdFromConnection(connectionId);
        
        if (ownerUserId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }
        String name = parts.get(1);
        Channel.Visibility visibility = Channel.Visibility.valueOf(parts.get(2));

        
        Channel channel = new Channel(null, name, ownerUserId, visibility, LocalDateTime.now());
        Channel saved = channelRepository.save(channel);
        channelRepository.addMember(saved.getId(), ownerUserId);
        

        return parser.encode("OK", String.valueOf(saved.getId()));
    }
}


