package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;
import com.chatCommon.dto.ChannelInviteDTO;
import com.chatCommon.dto.InviteStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ListPendingInvitesClientCommand implements ClientCommand<Void, List<ChannelInviteDTO>> {
    private final ServerGatewayPort gateway;
    public ListPendingInvitesClientCommand(ServerGatewayPort gateway) { this.gateway = gateway; }

    @Override
    public List<ChannelInviteDTO> execute(Void request) {
        try {
            List<String> parts = gateway.sendAndReceive("LIST_PENDING_INVITES");
            if (parts.isEmpty() || !"OK".equalsIgnoreCase(parts.getFirst())) return List.of();
            if (parts.size() < 2 || parts.get(1).isEmpty()) return List.of();
            String[] items = parts.get(1).split(";");
            List<ChannelInviteDTO> out = new ArrayList<>();
            for (String item : items) {
                String[] f = item.split(",");
                if (f.length >= 6) {
                    int inviteId = Integer.parseInt(f[0]);
                    int channelId = Integer.parseInt(f[1]);
                    String inviterUserId = f[2];
                    String inviterUsername = f[3];
                    String channelName = f[4];
                    out.add(new ChannelInviteDTO(inviteId, channelId, inviterUserId, inviterUsername, 
                                               null, channelName, InviteStatus.PENDING, LocalDateTime.now()));
                }
            }
            return out;
        } catch (Exception e) { return List.of(); }
    }
}


