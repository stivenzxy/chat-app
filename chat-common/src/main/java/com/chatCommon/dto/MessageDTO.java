package com.chatCommon.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MessageDTO implements Serializable {
    private String senderId;
    private final String recipientId;
    private final MessageType messageType;

    private final String textContent;
    private final byte[] audioContent;

    private final LocalDateTime timestamp;

    public MessageDTO(String senderId, String recipientId, String textContent) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.textContent = textContent;
        this.messageType = MessageType.TEXT;
        this.audioContent = null;
        this.timestamp = LocalDateTime.now();
    }

    public MessageDTO(String senderId, String recipientId, byte[] audioContent) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.audioContent = audioContent;
        this.messageType = MessageType.AUDIO;
        this.textContent = null;
        this.timestamp = LocalDateTime.now();
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getRecipientId() { return recipientId; }
    public MessageType getMessageType() { return messageType; }
    public String getTextContent() { return textContent; }
    public byte[] getAudioContent() { return audioContent; }
    public LocalDateTime getTimestamp() { return timestamp; }
}