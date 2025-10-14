package com.chatCommon.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MessageDTO implements Serializable {
    private final String senderId;
    private final String recipientId;
    private final MessageType messageType; // Nuevo campo

    // Contenido (solo uno de ellos se usará)
    private final String textContent;
    private final byte[] audioContent;

    private final LocalDateTime timestamp;

    // Constructor para mensajes de TEXTO
    public MessageDTO(String senderId, String recipientId, String textContent) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.textContent = textContent;
        this.messageType = MessageType.TEXT;
        this.audioContent = null; // No es audio
        this.timestamp = LocalDateTime.now();
    }

    // Constructor para mensajes de AUDIO
    public MessageDTO(String senderId, String recipientId, byte[] audioContent) {
        this.senderId = senderId;
        this.recipientId = recipientId;
        this.audioContent = audioContent;
        this.messageType = MessageType.AUDIO;
        this.textContent = null; // No es texto
        this.timestamp = LocalDateTime.now();
    }

    // Getters
    public String getSenderId() { return senderId; }
    public String getRecipientId() { return recipientId; }
    public MessageType getMessageType() { return messageType; }
    public String getTextContent() { return textContent; }
    public byte[] getAudioContent() { return audioContent; }
    public LocalDateTime getTimestamp() { return timestamp; }
}