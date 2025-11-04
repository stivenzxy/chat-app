package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.services.AudioTranscriptionService;

import java.util.Base64;
import java.util.List;

public class TranscribeAudioCommandAdapter implements ProtocolCommandAdapter {
    private final TcpServer server;
    private final AudioTranscriptionService audioTranscriptionService;
    private final MessageDAO messageDAO = new MessageDAO();

    public TranscribeAudioCommandAdapter(TcpServer server) {
        this.server = server;
        this.audioTranscriptionService = new AudioTranscriptionService();
    }

    @Override
    public String getCommandName() {
        return "TRANSCRIBE_AUDIO";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 2) {
            return parser.encode("ERROR", "Argumentos insuficientes para transcribir audio.");
        }

        String senderUserId = connectionContext.getId();
        String audioBase64 = parts.get(1);
        
        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String senderUsername = aum.getActiveUsers().entrySet().stream()
            .filter(entry -> entry.getValue().getId().equals(senderUserId))
            .map(entry -> entry.getKey())
            .findFirst()
            .orElse(null);
            
        if (senderUsername == null) {
            return parser.encode("ERROR", "Usuario no válido");
        }

        try {
            byte[] audioData = Base64.getDecoder().decode(audioBase64);
            
            int messageId = messageDAO.findAudioMessageByContent(senderUserId, audioData);
            
            if (messageId > 0) {
                String existingTranscription = messageDAO.getTranscription(messageId);
                if (existingTranscription != null && !existingTranscription.trim().isEmpty()) {
                    return parser.encode("TRANSCRIPTION_RESULT", existingTranscription);
                }
            }
            
            String transcribedText = audioTranscriptionService.transcribeAudio(audioData);

            if (transcribedText != null && !transcribedText.trim().isEmpty()) {
                if (messageId > 0) {
                    messageDAO.saveTranscription(messageId, "WAV", transcribedText);
                }

                return parser.encode("TRANSCRIPTION_RESULT", transcribedText);
            } else {
                return parser.encode("ERROR", "No se pudo transcribir...");
            }
        } catch (Exception e) {
            return parser.encode("ERROR", "Error interno durante la transcripción");
        }
    }
}
