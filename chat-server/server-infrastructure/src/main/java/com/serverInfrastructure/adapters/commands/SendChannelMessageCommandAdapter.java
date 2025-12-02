package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;

import java.util.List;
import java.util.Map;

import com.serverApplication.dto.sync.MessageSyncDTO;
import java.time.LocalDateTime;

public class SendChannelMessageCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;
    private final CommandHandler handler;
    private final MessageDAO messageDAO;
    private ServerNetworkAdapter networkAdapter;

    public SendChannelMessageCommandAdapter(ChannelRepository channelRepository, CommandHandler handler) {
        this.channelRepository = channelRepository;
        this.handler = handler;
        this.messageDAO = new MessageDAO();
    }

    public void setNetworkAdapter(ServerNetworkAdapter networkAdapter) {
        this.networkAdapter = networkAdapter;
    }

    @Override
    public String getCommandName() {
        return "SEND_CHANNEL_MESSAGE";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 3)
            return parser.encode("ERROR", "Argumentos insuficientes");

        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager
                .getInstance();
        String senderUserId = aum.getUserIdFromConnection(connectionContext.getId());

        if (senderUserId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }

        String channelId = parts.get(1);
        String content = parts.get(2);

        String senderUsername = aum.getAllUserSessions().entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(u -> {
                            String realUserId = aum.getUserIdFromConnection(u.getId());
                            return realUserId != null && realUserId.equals(senderUserId);
                        }))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        if (senderUsername == null) {
            return parser.encode("ERROR", "Usuario no encontrado");
        }

        if (!channelRepository.isMember(channelId, senderUserId)) {
            return parser.encode("ERROR", "No eres miembro del canal");
        }

        String messageId = null;
        if (!content.startsWith("[TRANSCRIPCIÓN]")) {
            messageId = messageDAO.saveChannelTextMessage(senderUserId, channelId, content);
        }

        if (messageId != null && networkAdapter != null) {
            MessageSyncDTO syncDTO = new MessageSyncDTO(
                    messageId,
                    senderUserId,
                    null,
                    channelId,
                    content,
                    "TEXT",
                    null,
                    LocalDateTime.now());
            networkAdapter.broadcastChannelMessage(syncDTO);
        }

        List<String> memberUsernames = channelRepository.findMemberUsernames(channelId);

        String forward = parser.encode("RECEIVE_CHANNEL_MESSAGE", channelId, senderUsername, content);

        for (String username : memberUsernames) {
            var userSessions = aum.getUserSessions(username);
            boolean isLocalUser = userSessions != null && !userSessions.isEmpty();

            if (isLocalUser) {
                handler.getServer().sendMessageToUser(username, forward, senderUsername);
            } else if (networkAdapter != null && networkAdapter.isUserConnected(username)) {
                String routeMessage = parser.encode("P2P_CHANNEL_MESSAGE",
                        channelId,
                        senderUsername,
                        content,
                        username);

                networkAdapter.routeChannelMessageToPeer(username, routeMessage);
            }
        }

        return parser.encode("OK", "Mensaje enviado");
    }
}
