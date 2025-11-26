package com.serverInfrastructure.persistence.dao;

import com.serverDomain.entities.ChannelInvite;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChannelInviteDAO {
    private static final Logger logger = LoggerFactory.getLogger(ChannelInviteDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public ChannelInvite insert(ChannelInvite invite) {
        String sql = "INSERT INTO channel_invites (invite_id, channel_id, inviter_user_id, invited_user_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        String inviteId = UUID.randomUUID().toString();

        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, inviteId);
            stmt.setString(2, invite.getChannelId());
            stmt.setString(3, invite.getInviterUserId());
            stmt.setString(4, invite.getInvitedUserId());
            stmt.setString(5, invite.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(invite.getCreatedAt()));
            stmt.executeUpdate();

            return new ChannelInvite(inviteId, invite.getChannelId(), invite.getInviterUserId(),
                    invite.getInvitedUserId(), invite.getStatus(), invite.getCreatedAt());
        } catch (SQLException e) {
            logger.error("Error al insertar invitación: {}", e.getMessage());
        }
        return invite;
    }

    public void updateStatus(String inviteId, ChannelInvite.Status status) {
        String sql = "UPDATE channel_invites SET status = ? WHERE invite_id = ?";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setString(2, inviteId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al actualizar estado de invitación: {}", e.getMessage());
        }
    }

    public List<ChannelInvite> findPendingForUser(String userId) {
        String sql = "SELECT ci.*, u.username as inviter_username, c.name as channel_name " +
                "FROM channel_invites ci " +
                "JOIN users u ON ci.inviter_user_id = u.user_id " +
                "JOIN channels c ON ci.channel_id = c.channel_id " +
                "WHERE ci.invited_user_id = ? AND ci.status = 'PENDING'";
        List<ChannelInvite> list = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next())
                    list.add(mapRowWithNames(rs));
            }
        } catch (SQLException e) {
            logger.error("Error al listar invitaciones pendientes: {}", e.getMessage());
        }
        return list;
    }

    private ChannelInvite mapRow(ResultSet rs) throws SQLException {
        String id = rs.getString("invite_id");
        String channelId = rs.getString("channel_id");
        String inviterUserId = rs.getString("inviter_user_id");
        String invitedUserId = rs.getString("invited_user_id");
        ChannelInvite.Status status = ChannelInvite.Status.valueOf(rs.getString("status"));
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new ChannelInvite(id, channelId, inviterUserId, invitedUserId, status, createdAt);
    }

    private ChannelInvite mapRowWithNames(ResultSet rs) throws SQLException {
        String id = rs.getString("invite_id");
        String channelId = rs.getString("channel_id");
        String inviterUserId = rs.getString("inviter_user_id");
        String invitedUserId = rs.getString("invited_user_id");
        String inviterUsername = rs.getString("inviter_username");
        String channelName = rs.getString("channel_name");
        ChannelInvite.Status status = ChannelInvite.Status.valueOf(rs.getString("status"));
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new ChannelInvite(id, channelId, inviterUserId, invitedUserId, inviterUsername, channelName, status,
                createdAt);
    }

    public List<ChannelInvite> findAll() {
        String sql = "SELECT * FROM channel_invites";
        List<ChannelInvite> invites = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                invites.add(new ChannelInvite(
                        rs.getString("invite_id"),
                        rs.getString("channel_id"),
                        rs.getString("inviter_user_id"),
                        rs.getString("invited_user_id"),
                        ChannelInvite.Status.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toLocalDateTime()));
            }
        } catch (SQLException e) {
            logger.error("Error al listar todas las invitaciones: {}", e.getMessage());
        }
        return invites;
    }

    public void insertReplicated(ChannelInvite invite) {
        String sql = "INSERT INTO channel_invites (invite_id, channel_id, inviter_user_id, invited_user_id, status, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invite.getId());
            stmt.setString(2, invite.getChannelId());
            stmt.setString(3, invite.getInviterUserId());
            stmt.setString(4, invite.getInvitedUserId());
            stmt.setString(5, invite.getStatus().name());
            stmt.setTimestamp(6, Timestamp.valueOf(invite.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error al insertar invitación replicada: {}", e.getMessage());
        }
    }
}
