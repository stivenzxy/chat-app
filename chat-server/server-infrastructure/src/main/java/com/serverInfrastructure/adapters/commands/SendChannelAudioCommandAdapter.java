package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.services.AudioTranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class SendChannelAudioCommandAdapter implements ProtocolCommandAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SendChannelAudioCommandAdapter.class);
    
    private final ChannelRepository channelRepository;
    private final CommandHandler handler;
    private final MessageDAO messageDAO;
    private final AudioTranscriptionService audioTranscriptionService = new AudioTranscriptionService();

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
        
        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String senderUserId = aum.getUserIdFromConnection(connectionContext.getId());
        
        if (senderUserId == null) {
            return parser.encode("ERROR", "Usuario no autenticado");
        }
        
        Integer channelId = Integer.valueOf(parts.get(1));
        String audioBase64 = parts.get(2);

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

        byte[] audioData = java.util.Base64.getDecoder().decode(audioBase64);
        
        int messageId = messageDAO.saveChannelAudioMessage(senderUserId, channelId, audioData);
        
        final int finalMessageId = messageId;
        final byte[] finalAudioData = audioData;
        if (finalMessageId > 0 && finalAudioData != null) {
            new Thread(() -> {
                try {
                    String transcribedText = audioTranscriptionService.transcribeAudio(finalAudioData);
                    
                    if (transcribedText != null && !transcribedText.trim().isEmpty()) {
                        messageDAO.saveTranscription(finalMessageId, "WAV", transcribedText);
                    }
                } catch (Exception e) {
                }
            }).start();
        }

        List<String> memberUsernames = channelRepository.findMemberUsernames(channelId);
        
        String forward = parser.encode("RECEIVE_CHANNEL_AUDIO", String.valueOf(channelId), senderUsername, audioBase64);
        
        // Enviar a todos los miembros del canal
        for (String username : memberUsernames) {
            handler.getServer().sendMessageToUser(username, forward, senderUsername + " [AUDIO]");
        }
        
        return parser.encode("OK", "Audio enviado");
    }
}


