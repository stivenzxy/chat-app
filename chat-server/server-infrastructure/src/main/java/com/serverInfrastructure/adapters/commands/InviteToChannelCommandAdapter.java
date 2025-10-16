package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;

import java.time.LocalDateTime;
import java.util.List;

public class InviteToChannelCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;
    private final ChannelInviteRepository inviteRepository;
    private final CommandHandler handler;

    public InviteToChannelCommandAdapter(ChannelRepository channelRepository, ChannelInviteRepository inviteRepository, CommandHandler handler) {
        this.channelRepository = channelRepository;
        this.inviteRepository = inviteRepository;
        this.handler = handler;
    }

    @Override
    public String getCommandName() { return "INVITE_TO_CHANNEL"; }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        // INVITE_TO_CHANNEL|channelId|invitedUsername
        if (parts.size() < 3) return parser.encode("ERROR", "Argumentos insuficientes");
        String inviterUserId = connectionContext.getId();
        Integer channelId = Integer.valueOf(parts.get(1));
        String invitedUsername = parts.get(2);

        var aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        
        // Obtener username del usuario que invita
        String inviterUsername = aum.getActiveUsers().entrySet().stream()
            .filter(entry -> entry.getValue().getId().equals(inviterUserId))
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
            
        if (inviterUsername == null) {
            return parser.encode("ERROR", "Usuario que invita no está en línea");
        }
        
        var invitedUser = aum.getActiveUsers().get(invitedUsername);
        if (invitedUser == null) return parser.encode("ERROR", "Usuario invitado no está en línea");

        String invitedUserId = invitedUser.getId();

        if (!channelRepository.isMember(channelId, inviterUserId)) {
            return parser.encode("ERROR", "No eres miembro del canal");
        }

        ChannelInvite invite = new ChannelInvite(null, channelId, inviterUserId, invitedUserId,
                ChannelInvite.Status.PENDING, LocalDateTime.now());
        ChannelInvite saved = inviteRepository.save(invite);

        var channelOpt = channelRepository.findById(channelId);
        String channelName = channelOpt.map(c -> c.getName()).orElse("Canal");
        String visibility = channelOpt.map(c -> c.getVisibility().name()).orElse("PUBLIC");

        String forward = parser.encode("INVITE_RECEIVED",
                String.valueOf(saved.getId()),
                String.valueOf(channelId),
                channelName,
                visibility,
                inviterUsername);
        handler.getServer().sendMessageToUser(invitedUsername, forward, inviterUsername);

        return parser.encode("OK", String.valueOf(saved.getId()));
    }
}


