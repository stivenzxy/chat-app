package com.clientInfrastructure.persistence.dao;

import com.chatCommon.dto.ChannelDTO;
import com.chatCommon.dto.ChannelVisibility;
import com.clientInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChannelDAO {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public void upsertChannel(ChannelDTO channel) {
        String sql = "MERGE INTO channels (channel_id, name, owner_id, visibility, created_at) KEY(channel_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel.getId());
            stmt.setString(2, channel.getName());
            stmt.setString(3, channel.getOwnerId());
            stmt.setString(4, channel.getVisibility().name());
            stmt.setTimestamp(5,
                    Timestamp.valueOf(channel.getCreatedAt() != null ? channel.getCreatedAt() : LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error upsert canal: {}", e.getMessage());
        }
    }

    public List<ChannelDTO> listChannels() {
        List<ChannelDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM channels ORDER BY name";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("channel_id");
                String name = rs.getString("name");
                String ownerId = rs.getString("owner_id");
                ChannelVisibility vis = ChannelVisibility.valueOf(rs.getString("visibility"));
                LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
                list.add(new ChannelDTO(id, name, ownerId, vis, created));
            }
        } catch (SQLException e) {
            logger.error("Error listando canales: {}", e.getMessage());
        }
        return list;
    }
}
