package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.factories.InfrastructureFactory;

import java.util.List;

public class RespondInviteCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;
    private final ChannelInviteRepository inviteRepository;
    private final CommandHandler handler;

    public RespondInviteCommandAdapter(ChannelRepository channelRepository, ChannelInviteRepository inviteRepository, CommandHandler handler) {
        this.channelRepository = channelRepository;
        this.inviteRepository = inviteRepository;
        this.handler = handler;
    }

    @Override
    public String getCommandName() { return "RESPOND_INVITE"; }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 4) return parser.encode("ERROR", "Argumentos insuficientes");
        Integer inviteId = Integer.valueOf(parts.get(1));
        ChannelInvite.Status status = ChannelInvite.Status.valueOf(parts.get(2));
        Integer channelId = Integer.valueOf(parts.get(3));
        
        String userId = ActiveUserManager.getInstance().getUserIdFromConnection(connectionContext.getId());
        if (userId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }


        inviteRepository.updateStatus(inviteId, status);
        if (status == ChannelInvite.Status.ACCEPTED) {
            channelRepository.addMember(channelId, userId);

            String newMemberUsername = ActiveUserManager.getInstance().getAllUserSessions().entrySet().stream()
                    .filter(entry -> entry.getValue().stream()
                        .anyMatch(u -> {
                            String realUserId = ActiveUserManager.getInstance().getUserIdFromConnection(u.getId());
                            return realUserId != null && realUserId.equals(userId);
                        }))
                    .map(java.util.Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (newMemberUsername != null) {
                List<String> memberUsernames = channelRepository.findMemberUsernames(channelId);
                String notification = parser.encode("CHANNEL_MEMBERS_UPDATED", String.valueOf(channelId), newMemberUsername);

                ActiveUserManager aum = ActiveUserManager.getInstance();
                ServerNetworkAdapter networkAdapter = InfrastructureFactory.getInstance().createServerNetworkAdapter();

                for (String memberUsername : memberUsernames) {
                    // Verificar si el usuario es local o remoto
                    var userSessions = aum.getUserSessions(memberUsername);
                    boolean isLocalUser = userSessions != null && !userSessions.isEmpty();
                    
                    if (isLocalUser) {
                        // Usuario local: enviar directamente
                        handler.getServer().sendMessageToUser(memberUsername, notification, "SERVER_NOTIFICATION");
                    } else {
                        // Usuario remoto: enrutar a través de P2P
                        // Formato: P2P_CHANNEL_MESSAGE|channelId|senderUsername|content|recipientUsername
                        // Usamos el mismo formato pero con un contenido especial para notificaciones
                        String routeMessage = parser.encode("P2P_CHANNEL_MESSAGE",
                                String.valueOf(channelId),
                                "SYSTEM",
                                "MEMBER_JOINED:" + newMemberUsername,
                                memberUsername);
                        
                        networkAdapter.routeChannelMessageToPeer(memberUsername, routeMessage);
                    }
                }
            }
        }
        return parser.encode("OK", "Invitación actualizada");
    }
}