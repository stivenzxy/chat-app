package com.serverInfrastructure.persistence.dao;

import com.serverInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;

public class MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public void saveChannelTextMessage(String authorId, int channelId, String content) {
        String sql = "INSERT INTO messages (author_id, recipient_channel_id, content, message_type, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, authorId);
            stmt.setInt(2, channelId);
            stmt.setString(3, content);
            stmt.setString(4, "TEXT");
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            
            logger.debug("Mensaje de texto de canal guardado: canal={}, autor={}", channelId, authorId);
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje de texto de canal: {}", e.getMessage());
        }
    }

    public void saveChannelAudioMessage(String authorId, int channelId, byte[] audioData) {
        String sql = "INSERT INTO messages (author_id, recipient_channel_id, message_type, audio_content, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, authorId);
            stmt.setInt(2, channelId);
            stmt.setString(3, "AUDIO");
            stmt.setBytes(4, audioData);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            
            logger.debug("Mensaje de audio de canal guardado: canal={}, autor={}", channelId, authorId);
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje de audio de canal: {}", e.getMessage());
        }
    }

    public void savePrivateTextMessage(String authorId, String recipientId, String content) {
        String sql = "INSERT INTO messages (author_id, recipient_user_id, content, message_type, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, authorId);
            stmt.setString(2, recipientId);
            stmt.setString(3, content);
            stmt.setString(4, "TEXT");
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            
            logger.debug("Mensaje privado de texto guardado: autor={}, destinatario={}", authorId, recipientId);
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje privado de texto: {}", e.getMessage());
        }
    }

    public void savePrivateAudioMessage(String authorId, String recipientId, byte[] audioData) {
        String sql = "INSERT INTO messages (author_id, recipient_user_id, message_type, audio_content, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); 
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, authorId);
            stmt.setString(2, recipientId);
            stmt.setString(3, "AUDIO");
            stmt.setBytes(4, audioData);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.executeUpdate();
            
            logger.debug("Mensaje privado de audio guardado: autor={}, destinatario={}", authorId, recipientId);
        } catch (SQLException e) {
            logger.error("Error al guardar mensaje privado de audio: {}", e.getMessage());
        }
    }
}
