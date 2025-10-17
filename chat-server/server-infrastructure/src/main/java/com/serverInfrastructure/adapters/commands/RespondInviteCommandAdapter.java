package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.services.CommandHandler;

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
        String userId = connectionContext.getId();


        inviteRepository.updateStatus(inviteId, status);
        if (status == ChannelInvite.Status.ACCEPTED) {
            channelRepository.addMember(channelId, userId);

            String newMemberUsername = ActiveUserManager.getInstance().getActiveUsers().entrySet().stream()
                    .filter(entry -> entry.getValue().getId().equals(userId))
                    .map(java.util.Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);

            if (newMemberUsername != null) {
                List<String> memberUsernames = channelRepository.findMemberUsernames(channelId);
                String notification = parser.encode("CHANNEL_MEMBERS_UPDATED", String.valueOf(channelId), newMemberUsername);

                for (String memberUsername : memberUsernames) {
                    handler.getServer().sendMessageToUser(memberUsername, notification, "SERVER_NOTIFICATION");
                }
            }
        }
        return parser.encode("OK", "Invitación actualizada");
    }
}