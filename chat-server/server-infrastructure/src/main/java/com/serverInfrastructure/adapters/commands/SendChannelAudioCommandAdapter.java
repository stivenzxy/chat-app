package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.persistence.dao.MessageDAO;

import java.util.List;

public class SendChannelAudioCommandAdapter implements ProtocolCommandAdapter {
    private final ChannelRepository channelRepository;
    private final CommandHandler handler;
    private final MessageDAO messageDAO;

    public SendChannelAudioCommandAdapter(ChannelRepository channelRepository, CommandHandler handler) {
        this.channelRepository = channelRepository;
        this.handler = handler;
        this.messageDAO = new MessageDAO();
    }

    @Override
    public String getCommandName() { return "SEND_CHANNEL_AUDIO"; }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        // SEND_CHANNEL_AUDIO|channelId|audioBase64
        if (parts.size() < 3) return parser.encode("ERROR", "Argumentos insuficientes");
        String senderUserId = connectionContext.getId();
        Integer channelId = Integer.valueOf(parts.get(1));
        String audioBase64 = parts.get(2);

        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String senderUsername = aum.getActiveUsers().entrySet().stream()
            .filter(entry -> entry.getValue().getId().equals(senderUserId))
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
            
        if (senderUsername == null) {
            return parser.encode("ERROR", "Usuario no encontrado");
        }

        if (!channelRepository.isMember(channelId, senderUserId)) {
            return parser.encode("ERROR", "No eres miembro del canal");
        }

        byte[] audioData = java.util.Base64.getDecoder().decode(audioBase64);
        
        messageDAO.saveChannelAudioMessage(senderUserId, channelId, audioData);

        List<String> memberUsernames = channelRepository.findMemberUsernames(channelId);
        
        String forward = parser.encode("RECEIVE_CHANNEL_AUDIO", String.valueOf(channelId), senderUsername, audioBase64);
        
        memberUsernames.stream()
            .filter(u -> !u.equals(senderUsername))
            .forEach(u -> {
                boolean sent = handler.getServer().sendMessageToUser(u, forward, senderUsername + " [AUDIO]");
            });
        
        return parser.encode("OK", "Audio enviado");
    }
}


