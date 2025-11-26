package com.serverInfrastructure.persistence.dao;

import com.serverDomain.entities.Channel;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ChannelDAO {
    private static final Logger logger = LoggerFactory.getLogger(ChannelDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public Channel insert(Channel channel) {
        String sql = "INSERT INTO channels (channel_id, name, owner_id, visibility, created_at) VALUES (?, ?, ?, ?, ?)";
        String channelId = UUID.randomUUID().toString();

        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            stmt.setString(2, channel.getName());
            stmt.setString(3, channel.getOwnerId());
            stmt.setString(4, channel.getVisibility().name());
            stmt.setTimestamp(5, Timestamp.valueOf(channel.getCreatedAt()));
            stmt.executeUpdate();

            return new Channel(channelId, channel.getName(), channel.getOwnerId(), channel.getVisibility(),
                    channel.getCreatedAt());
        } catch (SQLException e) {
            logger.error("Error al insertar canal: {}", e.getMessage());
        }
        return channel;
    }

    public Optional<Channel> findById(String id) {
        String sql = "SELECT * FROM channels WHERE channel_id = ?";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al buscar canal: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<Channel> findAllForUser(String userId) {
        String sql = "SELECT c.* FROM channels c JOIN channel_members m ON c.channel_id = m.channel_id WHERE m.user_id = ?";
        List<Channel> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar canales del usuario: {}", e.getMessage());
        }
        return list;
    }

    public void addMember(String channelId, String userId) {
        String sql = "INSERT IGNORE INTO channel_members (channel_id, user_id) VALUES (?, ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al agregar miembro al canal: {}", e.getMessage());
        }
    }

    public boolean isMember(String channelId, String userId) {
        String sql = "SELECT 1 FROM channel_members WHERE channel_id = ? AND user_id = ?";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            stmt.setString(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            logger.error("Error al verificar membresía: {}", e.getMessage());
            return false;
        }
    }

    public List<String> findMemberUserIds(String channelId) {
        String sql = "SELECT user_id FROM channel_members WHERE channel_id = ?";
        List<String> users = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    users.add(rs.getString("user_id"));
            }
        } catch (SQLException e) {
            logger.error("Error al listar miembros del canal: {}", e.getMessage());
        }
        return users;
    }

    public List<String> findMemberUsernames(String channelId) {
        String sql = "SELECT u.username FROM channel_members m JOIN users u ON u.user_id = m.user_id WHERE m.channel_id = ?";
        List<String> usernames = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channelId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    usernames.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            logger.error("Error listando usernames del canal: {}", e.getMessage());
        }
        return usernames;
    }

    public List<Channel> findAll() {
        String sql = "SELECT * FROM channels ORDER BY name";
        List<Channel> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar todos los canales: {}", e.getMessage());
        }
        return list;
    }

    private Channel mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("channel_id");
        String name = rs.getString("name");
        String ownerId = rs.getString("owner_id");
        Channel.Visibility visibility = Channel.Visibility.valueOf(rs.getString("visibility"));
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new Channel(id, name, ownerId, visibility, createdAt);
    }

    public void insertReplicated(Channel channel) {
        String sql = "INSERT INTO channels (channel_id, name, owner_id, visibility, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, channel.getId());
            stmt.setString(2, channel.getName());
            stmt.setString(3, channel.getOwnerId());
            stmt.setString(4, channel.getVisibility().name());
            stmt.setTimestamp(5, Timestamp.valueOf(channel.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al insertar canal replicado: {}", e.getMessage());
        }
    }

    public List<com.serverDomain.valueObjects.ChannelMember> findAllMembers() {
        String sql = "SELECT channel_id, user_id FROM channel_members";
        List<com.serverDomain.valueObjects.ChannelMember> members = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                members.add(new com.serverDomain.valueObjects.ChannelMember(
                        rs.getString("channel_id"),
                        rs.getString("user_id")));
            }
        } catch (SQLException e) {
            logger.error("Error al listar todos los miembros de canales: {}", e.getMessage());
        }
        return members;
    }
}
