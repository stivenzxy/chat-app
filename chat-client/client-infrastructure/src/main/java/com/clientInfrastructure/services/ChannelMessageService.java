package com.clientInfrastructure.services;

import com.chatCommon.dto.MessageDTO;
import com.clientInfrastructure.persistence.dao.MessageDAO;
import java.time.LocalDateTime;
import java.util.List;

public class ChannelMessageService {
    private final MessageDAO messageDAO;

    public ChannelMessageService() {
        this.messageDAO = new MessageDAO();
    }

    public void saveChannelTextMessage(String senderId, String channelId, String content) {
        messageDAO.saveChannelTextMessage(senderId, channelId, content, LocalDateTime.now());
    }

    public void saveChannelAudioMessage(String senderId, String channelId, byte[] audioData) {
        messageDAO.saveChannelAudioMessage(senderId, channelId, audioData, LocalDateTime.now());
    }

    public List<MessageDTO> getChannelHistory(String channelId) {
        return messageDAO.getChannelHistory(channelId);
    }
}
