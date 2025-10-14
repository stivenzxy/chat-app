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
                // --- INICIO DE LA LÓGICA FALTANTE ---
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
                // --- FIN DE LA LÓGICA FALTANTE ---
            }
        } catch (SQLException e) {
            logger.error("Error al cargar el historial de chat: {}", e.getMessage());
        }
        return history;
    }
}