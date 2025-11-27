package com.serverInfrastructure.persistence.dao;

import com.serverInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.sql.*;
import java.time.LocalDateTime;

public class MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public String saveChannelTextMessage(String authorId, String channelId, String content) {
        String sql = "INSERT INTO messages (message_id, author_id, recipient_channel_id, content, message_type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String messageId = UUID.randomUUID().toString();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            stmt.setString(2, authorId);
            stmt.setString(3, channelId);
            stmt.setString(4, content);
            stmt.setString(5, "TEXT");
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            logger.debug("Mensaje de texto de canal guardado: canal={}, autor={}, message_id={}", channelId, authorId,
                    messageId);
            return messageId;
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje de texto de canal: {}", e.getMessage());
            return null;
        }
    }

    public String saveChannelAudioMessage(String authorId, String channelId, byte[] audioData) {
        String sql = "INSERT INTO messages (message_id, author_id, recipient_channel_id, message_type, audio_content, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String messageId = UUID.randomUUID().toString();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            stmt.setString(2, authorId);
            stmt.setString(3, channelId);
            stmt.setString(4, "AUDIO");
            stmt.setBytes(5, audioData);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            logger.debug("Mensaje de audio de canal guardado: canal={}, autor={}, message_id={}", channelId, authorId,
                    messageId);
            return messageId;
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje de audio de canal: {}", e.getMessage());
            return null;
        }
    }

    public String savePrivateTextMessage(String authorId, String recipientId, String content) {
        String sql = "INSERT INTO messages (message_id, author_id, recipient_user_id, content, message_type, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String messageId = UUID.randomUUID().toString();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            stmt.setString(2, authorId);
            stmt.setString(3, recipientId);
            stmt.setString(4, content);
            stmt.setString(5, "TEXT");
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            logger.debug("Mensaje privado de texto guardado: autor={}, destinatario={}, message_id={}", authorId,
                    recipientId, messageId);
            return messageId;
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje privado de texto: {}", e.getMessage());
            return null;
        }
    }

    public String savePrivateAudioMessage(String authorId, String recipientId, byte[] audioData) {
        String sql = "INSERT INTO messages (message_id, author_id, recipient_user_id, message_type, audio_content, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String messageId = UUID.randomUUID().toString();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            stmt.setString(2, authorId);
            stmt.setString(3, recipientId);
            stmt.setString(4, "AUDIO");
            stmt.setBytes(5, audioData);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();

            logger.debug("Mensaje privado de audio guardado: autor={}, destinatario={}, message_id={}", authorId,
                    recipientId, messageId);
            return messageId;
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje privado de audio: {}", e.getMessage());
            return null;
        }
    }

    public boolean saveTranscription(String messageId, String audioFormat, String transcribedText) {
        String sql = "INSERT INTO audio_transcriptions (message_id, audio_format, transcribed_text) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE audio_format = VALUES(audio_format), transcribed_text = VALUES(transcribed_text)";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);
            stmt.setString(2, audioFormat);
            stmt.setString(3, transcribedText);
            stmt.executeUpdate();

            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public String getTranscription(String messageId) {
        String sql = "SELECT transcribed_text FROM audio_transcriptions WHERE message_id = ?";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, messageId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("transcribed_text");
                }
            }

            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public String findAudioMessageByContent(String userId, byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return null;
        }

        String sql = "SELECT message_id, audio_content FROM messages " +
                "WHERE message_type = 'AUDIO' " +
                "AND (author_id = ? OR recipient_user_id = ?) " +
                "ORDER BY created_at DESC";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setString(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    byte[] storedAudio = rs.getBytes("audio_content");
                    if (storedAudio != null && java.util.Arrays.equals(audioData, storedAudio)) {
                        return rs.getString("message_id");
                    }
                }
            }

            return null;
        } catch (SQLException e) {
            return null;
        }
    }

    public String findRecentAudioMessage(String authorId, int minutesAgo) {
        String sql = "SELECT message_id, recipient_user_id, recipient_channel_id FROM messages " +
                "WHERE author_id = ? AND message_type = 'AUDIO' " +
                "AND created_at >= DATE_SUB(NOW(), INTERVAL ? MINUTE) " +
                "ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, authorId);
            stmt.setInt(2, minutesAgo);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String messageId = rs.getString("message_id");
                    String recipientChannelId = rs.getString("recipient_channel_id");
                    String messageType = recipientChannelId != null ? "canal" : "privado";
                    logger.debug("Mensaje de audio reciente encontrado: message_id={}, author={}, tipo={}", messageId,
                            authorId, messageType);
                    return messageId;
                }
            }
        } catch (SQLException e) {
            logger.error("Error al buscar mensaje de audio reciente: {}", e.getMessage());
            return null;
        }

        String sqlFallback = "SELECT message_id, recipient_user_id, recipient_channel_id FROM messages " +
                "WHERE author_id = ? AND message_type = 'AUDIO' " +
                "ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sqlFallback)) {

            stmt.setString(1, authorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String messageId = rs.getString("message_id");
                    String recipientChannelId = rs.getString("recipient_channel_id");
                    String messageType = recipientChannelId != null ? "canal" : "privado";
                    logger.debug(
                            "Mensaje de audio más reciente encontrado (sin restricción de tiempo): message_id={}, author={}, tipo={}",
                            messageId, authorId, messageType);
                    return messageId;
                }
            }

            logger.warn("No se encontró ningún mensaje de audio (privado o de canal) para author_id={}", authorId);
            return null;
        } catch (SQLException e) {
            logger.error("Error al buscar mensaje de audio (fallback): {}", e.getMessage());
            return null;
        }
    }

    public Map<String, String> getAllTranscriptions() {
        Map<String, String> transcriptions = new LinkedHashMap<>();
        String sql = "SELECT u.username, at.transcribed_text AS content, m.created_at, " +
                "c.name AS channel_name, u2.username AS recipient_username " +
                "FROM audio_transcriptions at " +
                "JOIN messages m ON at.message_id = m.message_id " +
                "JOIN users u ON m.author_id = u.user_id " +
                "LEFT JOIN channels c ON m.recipient_channel_id = c.channel_id " +
                "LEFT JOIN users u2 ON m.recipient_user_id = u2.user_id " +
                "ORDER BY m.created_at ASC, m.message_id ASC";

        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                String content = rs.getString("content");
                Timestamp createdAt = rs.getTimestamp("created_at");
                String channelName = rs.getString("channel_name");
                String recipientUsername = rs.getString("recipient_username");

                String comunicacion;
                if (channelName != null && !channelName.trim().isEmpty()) {
                    comunicacion = "Canal " + channelName;
                } else if (recipientUsername != null && !recipientUsername.trim().isEmpty()) {
                    comunicacion = "Chat con " + recipientUsername;
                } else {
                    comunicacion = "Chat Privado";
                }

                String key = username + " ("
                        + createdAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) + ") ["
                        + comunicacion + "]";
                transcriptions.put(key, content);
            }
        } catch (SQLException e) {
        }
        return transcriptions;
    }

    public java.util.List<com.serverApplication.dto.sync.MessageSyncDTO> findAll() {
        String sql = "SELECT * FROM messages";
        java.util.List<com.serverApplication.dto.sync.MessageSyncDTO> messages = new java.util.ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String recipientChannelId = rs.getString("recipient_channel_id");
                messages.add(new com.serverApplication.dto.sync.MessageSyncDTO(
                        rs.getString("message_id"),
                        rs.getString("author_id"),
                        rs.getString("recipient_user_id"),
                        recipientChannelId,
                        rs.getString("content"),
                        rs.getString("message_type"),
                        rs.getBytes("audio_content"),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        } catch (SQLException e) {
            logger.error("Error al listar todos los mensajes: {}", e.getMessage());
        }
        return messages;
    }

    public java.util.List<com.serverApplication.dto.sync.MessageSyncDTO> getChannelMessages(String channelId) {
        String sql = "SELECT * FROM messages WHERE recipient_channel_id = ? ORDER BY created_at ASC";
        java.util.List<com.serverApplication.dto.sync.MessageSyncDTO> messages = new java.util.ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, channelId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(new com.serverApplication.dto.sync.MessageSyncDTO(
                            rs.getString("message_id"),
                            rs.getString("author_id"),
                            rs.getString("recipient_user_id"),
                            rs.getString("recipient_channel_id"),
                            rs.getString("content"),
                            rs.getString("message_type"),
                            rs.getBytes("audio_content"),
                            rs.getTimestamp("created_at").toLocalDateTime()));
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener mensajes del canal {}: {}", channelId, e.getMessage());
        }
        return messages;
    }

    public java.util.List<com.serverApplication.dto.sync.AudioTranscriptionSyncDTO> findAllTranscriptions() {
        String sql = "SELECT * FROM audio_transcriptions";
        java.util.List<com.serverApplication.dto.sync.AudioTranscriptionSyncDTO> transcriptions = new java.util.ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                transcriptions.add(new com.serverApplication.dto.sync.AudioTranscriptionSyncDTO(
                        rs.getString("message_id"),
                        rs.getString("audio_format"),
                        rs.getString("transcribed_text")));
            }
        } catch (SQLException e) {
            logger.error("Error al listar todas las transcripciones: {}", e.getMessage());
        }
        return transcriptions;
    }

    public void insertReplicated(com.serverApplication.dto.sync.MessageSyncDTO message) {
        String sql = "INSERT INTO messages (message_id, author_id, recipient_user_id, recipient_channel_id, content, message_type, audio_content, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.messageId());
            stmt.setString(2, message.authorId());
            stmt.setString(3, message.recipientUserId());
            if (message.recipientChannelId() != null) {
                stmt.setString(4, message.recipientChannelId());
            } else {
                stmt.setNull(4, Types.VARCHAR);
            }
            stmt.setString(5, message.content());
            stmt.setString(6, message.messageType());
            stmt.setBytes(7, message.audioContent());
            stmt.setTimestamp(8, Timestamp.valueOf(message.createdAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al insertar mensaje replicado: {}", e.getMessage());
        }
    }

    public void insertTranscriptionReplicated(com.serverApplication.dto.sync.AudioTranscriptionSyncDTO transcription) {
        String sql = "INSERT INTO audio_transcriptions (message_id, audio_format, transcribed_text) VALUES (?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, transcription.messageId());
            stmt.setString(2, transcription.audioFormat());
            stmt.setString(3, transcription.transcribedText());
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al insertar transcripción replicada: {}", e.getMessage());
        }
    }
}
