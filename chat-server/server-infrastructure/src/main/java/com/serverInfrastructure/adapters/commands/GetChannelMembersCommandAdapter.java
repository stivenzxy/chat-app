package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.observers.ActiveUserManager;

import java.util.List;

public class GetChannelMembersCommandAdapter implements ProtocolCommandAdapter {

    private final ChannelRepository channelRepository;

    public GetChannelMembersCommandAdapter(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
    }

    @Override
    public String getCommandName() {
        return "GET_CHANNEL_MEMBERS";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        try {
            if (parts.size() < 2) {
                return parser.encode("ERROR", "Formato: GET_CHANNEL_MEMBERS|channelId");
            }

            String channelId = parts.get(1);
            String requesterId = ActiveUserManager.getInstance().getUserIdFromConnection(connectionContext.getId());

            if (requesterId == null) {
                return parser.encode("ERROR", "Usuario no autenticado");
            }

            boolean isMember = channelRepository.isMember(channelId, requesterId);

            if (!isMember) {
                return parser.encode("ERROR", "No eres miembro de este canal");
            }

            List<String> memberUsernames = channelRepository.findMemberUsernames(channelId);

            String membersPayload = String.join(",", memberUsernames);
            return parser.encode("OK", membersPayload);

        } catch (Exception e) {
            return parser.encode("ERROR", "Error obteniendo miembros: " + e.getMessage());
        }
    }
}
