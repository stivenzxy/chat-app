package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;

import java.util.List;

public class RespondInviteCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;
    private final ChannelInviteRepository inviteRepository;

    public RespondInviteCommandAdapter(ChannelRepository channelRepository, ChannelInviteRepository inviteRepository) {
        this.channelRepository = channelRepository;
        this.inviteRepository = inviteRepository;
    }

    @Override
    public String getCommandName() { return "RESPOND_INVITE"; }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        // RESPOND_INVITE|inviteId|ACCEPTED|REJECTED|channelId
        if (parts.size() < 4) return parser.encode("ERROR", "Argumentos insuficientes");
        Integer inviteId = Integer.valueOf(parts.get(1));
        ChannelInvite.Status status = ChannelInvite.Status.valueOf(parts.get(2));
        Integer channelId = Integer.valueOf(parts.get(3));
        String userId = connectionContext.getId();


        inviteRepository.updateStatus(inviteId, status);
        if (status == ChannelInvite.Status.ACCEPTED) {
            channelRepository.addMember(channelId, userId);
        }
        return parser.encode("OK", "Invitación actualizada");
    }
}


