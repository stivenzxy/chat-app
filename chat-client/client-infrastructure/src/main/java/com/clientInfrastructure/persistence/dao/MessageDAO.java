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

    // Guardar mensaje de canal (usa recipient_channel_id)
    public void saveChannelTextMessage(String senderId, int channelId, String content, LocalDateTime timestamp) {
        System.out.println("DEBUG: Guardando mensaje de canal - sender: " + senderId + ", canal: " + channelId + ", contenido: " + content);
        String sql = "INSERT INTO messages (sender_id, recipient_channel_id, content, message_type, sent_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            System.out.println("DEBUG: Conexión establecida, preparando statement: " + sql);
            stmt.setString(1, senderId);
            stmt.setInt(2, channelId);
            stmt.setString(3, content);
            stmt.setString(4, MessageType.TEXT.name());
            stmt.setTimestamp(5, Timestamp.valueOf(timestamp));
            System.out.println("DEBUG: Parámetros establecidos, ejecutando update...");
            int rowsAffected = stmt.executeUpdate();
            System.out.println("DEBUG: Mensaje de canal guardado exitosamente - filas afectadas: " + rowsAffected);
        } catch (SQLException e) { 
            logger.error("Error al guardar mensaje de canal: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public void saveChannelAudioMessage(String senderId, int channelId, byte[] audio, LocalDateTime timestamp) {
        String sql = "INSERT INTO messages (sender_id, recipient_channel_id, message_type, audio_content, sent_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, senderId);
            stmt.setInt(2, channelId);
            stmt.setString(3, MessageType.AUDIO.name());
            stmt.setBytes(4, audio);
            stmt.setTimestamp(5, Timestamp.valueOf(timestamp));
            stmt.executeUpdate();
        } catch (SQLException e) { logger.error("Error al guardar audio de canal: {}", e.getMessage()); }
    }

    public List<MessageDTO> getChannelHistory(int channelId) {
        System.out.println("DEBUG: getChannelHistory - buscando mensajes para canal " + channelId);
        List<MessageDTO> history = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE recipient_channel_id = ? ORDER BY sent_at ASC";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            System.out.println("DEBUG: Conexión establecida para consulta, preparando statement: " + sql);
            stmt.setInt(1, channelId);
            System.out.println("DEBUG: Parámetro establecido: channelId = " + channelId);
            ResultSet rs = stmt.executeQuery();
            System.out.println("DEBUG: Query ejecutado, procesando resultados...");
            int count = 0;
            while (rs.next()) {
                count++;
                String senderId = rs.getString("sender_id");
                String textContent = rs.getString("content");
                byte[] audioContent = rs.getBytes("audio_content");
                MessageType messageType = MessageType.valueOf(rs.getString("message_type"));
                System.out.println("DEBUG: Mensaje encontrado - sender: " + senderId + ", tipo: " + messageType + ", contenido: " + textContent);
                if (messageType == MessageType.TEXT) {
                    history.add(new MessageDTO(senderId, String.valueOf(channelId), textContent));
                } else {
                    history.add(new MessageDTO(senderId, String.valueOf(channelId), audioContent));
                }
            }
            System.out.println("DEBUG: getChannelHistory - encontrados " + count + " mensajes para canal " + channelId);
        } catch (SQLException e) { 
            logger.error("Error al cargar historial de canal: {}", e.getMessage());
            e.printStackTrace();
        }
        return history;
    }
}