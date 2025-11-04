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
        
        var aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String inviterUserId = aum.getUserIdFromConnection(connectionContext.getId());
        
        if (inviterUserId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }
        Integer channelId = Integer.valueOf(parts.get(1));
        String invitedUsername = parts.get(2);

        String inviterUsername = aum.getAllUserSessions().entrySet().stream()
            .filter(entry -> entry.getValue().stream()
                .anyMatch(u -> aum.getUserIdFromConnection(u.getId()) != null && 
                              aum.getUserIdFromConnection(u.getId()).equals(inviterUserId)))
            .map(java.util.Map.Entry::getKey)
            .findFirst()
            .orElse(null);
            
        if (inviterUsername == null) {
            return parser.encode("ERROR", "Usuario que invita no está en línea");
        }

        var invitedUserSessions = aum.getUserSessions(invitedUsername);
        if (invitedUserSessions == null || invitedUserSessions.isEmpty()) {
            return parser.encode("ERROR", "Usuario invitado no está en línea");
        }

        // Obtener el userId real del primer sesión del usuario invitado
        String invitedUserId = aum.getUserIdFromConnection(invitedUserSessions.get(0).getId());
        if (invitedUserId == null) {
            return parser.encode("ERROR", "No se pudo obtener ID del usuario invitado");
        }

        if (channelRepository.isMember(channelId, invitedUserId)) {
            return parser.encode("ERROR", "El usuario ya es miembro de este canal");
        }

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


