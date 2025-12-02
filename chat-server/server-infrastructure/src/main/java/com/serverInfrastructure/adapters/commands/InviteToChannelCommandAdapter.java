package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.persistence.dao.UserDAO;
import com.serverInfrastructure.observers.ActiveUserManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class InviteToChannelCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;
    private final ChannelInviteRepository inviteRepository;
    private final CommandHandler handler;
    private final UserDAO userDAO;
    private ServerNetworkAdapter networkAdapter;

    public InviteToChannelCommandAdapter(ChannelRepository channelRepository, ChannelInviteRepository inviteRepository,
            CommandHandler handler, UserDAO userDAO) {
        this.channelRepository = channelRepository;
        this.inviteRepository = inviteRepository;
        this.handler = handler;
        this.userDAO = userDAO;
    }

    public void setNetworkAdapter(ServerNetworkAdapter networkAdapter) {
        this.networkAdapter = networkAdapter;
    }

    @Override
    public String getCommandName() {
        return "INVITE_TO_CHANNEL";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 3)
            return parser.encode("ERROR", "Argumentos insuficientes");

        var aum = ActiveUserManager.getInstance();
        String inviterUserId = aum.getUserIdFromConnection(connectionContext.getId());

        if (inviterUserId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }
        String channelId = parts.get(1);
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

        boolean isUserConnected = false;
        if (networkAdapter != null) {
            isUserConnected = networkAdapter.isUserConnected(invitedUsername);
        } else {
            var invitedUserSessions = aum.getUserSessions(invitedUsername);
            isUserConnected = invitedUserSessions != null && !invitedUserSessions.isEmpty();
        }

        if (!isUserConnected) {
            return parser.encode("ERROR", "Usuario invitado no está en línea");
        }

        var invitedUserSessions = aum.getUserSessions(invitedUsername);
        boolean isLocalUser = invitedUserSessions != null && !invitedUserSessions.isEmpty();

        String invitedUserId;
        String actualInvitedUsername = invitedUsername;

        if (invitedUsername.contains(" - ") && invitedUsername.startsWith("Servidor ")) {
            actualInvitedUsername = invitedUsername.split(" - ", 2)[1];
        }

        if (isLocalUser) {
            invitedUserId = aum.getUserIdFromConnection(invitedUserSessions.get(0).getId());
            if (invitedUserId == null) {
                return parser.encode("ERROR", "No se pudo obtener ID del usuario invitado");
            }
        } else {
            Optional<User> userOpt = userDAO
                    .findByUsername(new com.serverDomain.valueObjects.Username(actualInvitedUsername));

            if (userOpt.isPresent()) {
                invitedUserId = userOpt.get().getId();
            } else {
                invitedUserId = "remote:" + actualInvitedUsername;
            }
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

        if (networkAdapter != null && saved.getId() != null) {
            networkAdapter.broadcastChannelInvite(saved);
        }

        String inviteId = saved.getId();
        if (inviteId == null) {
            inviteId = java.util.UUID.randomUUID().toString();
        }

        var channelOpt = channelRepository.findById(channelId);
        String channelName = channelOpt.map(c -> c.getName()).orElse("Canal");
        String visibility = channelOpt.map(c -> c.getVisibility().name()).orElse("PUBLIC");

        String forward = parser.encode("INVITE_RECEIVED",
                inviteId,
                channelId,
                channelName,
                visibility,
                inviterUsername);

        if (isLocalUser) {
            handler.getServer().sendMessageToUser(invitedUsername, forward, inviterUsername);
        } else if (networkAdapter != null) {
            
            String routeMessage = parser.encode("P2P_CHANNEL_INVITE",
                    inviteId,
                    channelId,
                    channelName,
                    visibility,
                    inviterUsername,
                    actualInvitedUsername);

            boolean routed = networkAdapter.routeChannelInviteToPeer(actualInvitedUsername, routeMessage);
            if (!routed) {
                return parser.encode("ERROR", "No se pudo enviar invitación al usuario remoto");
            }
        }

        return parser.encode("OK", inviteId);
    }
}
