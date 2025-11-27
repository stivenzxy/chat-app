package com.clientInfrastructure.services;

import com.chatCommon.dto.MessageDTO;
import com.clientInfrastructure.persistence.dao.MessageDAO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service responsible for channel message operations.
 */
public class ChannelMessageService {
    private final MessageDAO messageDAO;

    public ChannelMessageService() {
        this.messageDAO = new MessageDAO();
    }

    /**
     * Saves a channel text message to the database.
     */
    public void saveChannelTextMessage(String senderId, String channelId, String content) {
        messageDAO.saveChannelTextMessage(senderId, channelId, content, LocalDateTime.now());
    }

    /**
     * Saves a channel audio message to the database.
     */
    public void saveChannelAudioMessage(String senderId, String channelId, byte[] audioData) {
        messageDAO.saveChannelAudioMessage(senderId, channelId, audioData, LocalDateTime.now());
    }

    /**
     * Retrieves channel message history.
     */
    public List<MessageDTO> getChannelHistory(String channelId) {
        return messageDAO.getChannelHistory(channelId);
    }
}
