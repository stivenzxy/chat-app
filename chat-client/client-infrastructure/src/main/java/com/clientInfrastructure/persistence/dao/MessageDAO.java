package com.clientInfrastructure.persistence.dao;

import com.chatCommon.dto.MessageDTO;
import com.clientInfrastructure.persistence.config.ConnectionManager;
import com.chatCommon.dto.MessageType;
import java.time.LocalDateTime;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public void saveMessage(MessageDTO message) {
        if (messageExists(message)) {
            logger.debug("Mensaje duplicado detectado, no se guardará: {} -> {}", message.getSenderId(),
                    message.getRecipientId());
            return;
        }

        String sql = "INSERT INTO messages (sender_id, recipient_id, content, message_type, audio_content, sent_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.getSenderId());
            stmt.setString(2, message.getRecipientId());
            stmt.setString(3, message.getTextContent());
            stmt.setString(4, message.getMessageType().name());
            stmt.setBytes(5, message.getAudioContent());
            stmt.setTimestamp(6, Timestamp.valueOf(message.getTimestamp()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al guardar el mensaje: {}", e.getMessage());
        }
    }

    private boolean messageExists(MessageDTO message) {
        String sql;
        if (message.getMessageType() == MessageType.TEXT) {
            sql = "SELECT COUNT(*) FROM messages WHERE sender_id = ? AND recipient_id = ? AND message_type = ? " +
                    "AND content = ? AND ABS(DATEDIFF('SECOND', sent_at, ?)) <= 2";
        } else {
            sql = "SELECT COUNT(*) FROM messages WHERE sender_id = ? AND recipient_id = ? AND message_type = ? " +
                    "AND length(audio_content) = ? AND ABS(DATEDIFF('SECOND', sent_at, ?)) <= 2";
        }

        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, message.getSenderId());
            stmt.setString(2, message.getRecipientId());
            stmt.setString(3, message.getMessageType().name());

            if (message.getMessageType() == MessageType.TEXT) {
                stmt.setString(4, message.getTextContent());
            } else {
                stmt.setInt(4, message.getAudioContent() != null ? message.getAudioContent().length : 0);
            }
            stmt.setTimestamp(5, Timestamp.valueOf(message.getTimestamp()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    logger.debug("Mensaje duplicado encontrado: {} -> {} (count: {})",
                            message.getSenderId(), message.getRecipientId(), count);
                }
                return count > 0;
            }
        } catch (SQLException e) {
            logger.error("Error al verificar existencia del mensaje: {}", e.getMessage(), e);
        }
        return false;
    }

    public List<MessageDTO> getChatHistory(String user1, String user2) {
        List<MessageDTO> history = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE (sender_id = ? AND recipient_id = ?) OR (sender_id = ? AND recipient_id = ?) ORDER BY sent_at ASC";

        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user1);
            stmt.setString(2, user2);
            stmt.setString(3, user2);
            stmt.setString(4, user1);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String senderId = rs.getString("sender_id");
                String recipientId = rs.getString("recipient_id");
                String textContent = rs.getString("content");
                byte[] audioContent = rs.getBytes("audio_content");
                MessageType messageType = MessageType.valueOf(rs.getString("message_type"));
                LocalDateTime timestamp = rs.getTimestamp("sent_at").toLocalDateTime();

                MessageDTO message;
                if (messageType == MessageType.TEXT) {
                    message = new MessageDTO(senderId, recipientId, textContent);
                } else {
                    message = new MessageDTO(senderId, recipientId, audioContent);
                }
                history.add(message);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener historial de chat: {}", e.getMessage());
        }
        return history;
    }

    public void saveChannelTextMessage(String senderId, String channelId, String content, LocalDateTime timestamp) {
        String sql = "INSERT INTO channel_messages (channel_id, sender_user_id, content, message_type, sent_at) VALUES (?, ?, ?, 'TEXT', ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            stmt.setString(2, senderId);
            stmt.setString(3, content);
            stmt.setTimestamp(4, Timestamp.valueOf(timestamp));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error guardando mensaje de canal: {}", e.getMessage());
        }
    }

    public void saveChannelAudioMessage(String senderId, String channelId, byte[] audioData, LocalDateTime timestamp) {
        String sql = "INSERT INTO channel_messages (channel_id, sender_user_id, audio_content, message_type, sent_at) VALUES (?, ?, ?, 'AUDIO', ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            stmt.setString(2, senderId);
            stmt.setBytes(3, audioData);
            stmt.setTimestamp(4, Timestamp.valueOf(timestamp));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error guardando audio de canal: {}", e.getMessage());
        }
    }

    public List<MessageDTO> getChannelHistory(String channelId) {
        List<MessageDTO> history = new ArrayList<>();
        String sql = "SELECT * FROM channel_messages WHERE channel_id = ? ORDER BY sent_at ASC";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String sender = rs.getString("sender_user_id");
                String type = rs.getString("message_type");
                LocalDateTime time = rs.getTimestamp("sent_at").toLocalDateTime();
                if ("TEXT".equals(type)) {
                    String content = rs.getString("content");
                    history.add(new MessageDTO(sender, channelId, content));
                } else {
                    byte[] audio = rs.getBytes("audio_content");
                    history.add(new MessageDTO(sender, channelId, audio));
                }
            }
        } catch (SQLException e) {
            logger.error("Error obteniendo historial de canal: {}", e.getMessage());
        }
        return history;
    }
}