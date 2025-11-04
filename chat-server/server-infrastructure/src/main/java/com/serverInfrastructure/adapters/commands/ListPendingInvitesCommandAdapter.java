package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;

import java.util.List;
import java.util.stream.Collectors;

public class ListPendingInvitesCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelInviteRepository inviteRepository;

    public ListPendingInvitesCommandAdapter(ChannelInviteRepository inviteRepository) {
        this.inviteRepository = inviteRepository;
    }

    @Override
    public String getCommandName() { return "LIST_PENDING_INVITES"; }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        var aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String userId = aum.getUserIdFromConnection(connectionContext.getId());
        
        if (userId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }
        
        List<ChannelInvite> invites = inviteRepository.findPendingForUser(userId);
        
        String payload = invites.stream()
                .map(i -> i.getId() + "," + i.getChannelId() + "," + i.getInviterUserId() + "," + 
                         i.getInviterUsername() + "," + i.getChannelName() + ",PENDING")
                .collect(Collectors.joining(";"));
        return parser.encode("OK", payload);
    }
}


