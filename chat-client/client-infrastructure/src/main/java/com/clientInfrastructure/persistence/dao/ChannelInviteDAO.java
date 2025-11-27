package com.clientInfrastructure.persistence.dao;

import com.chatCommon.dto.ChannelInviteDTO;
import com.chatCommon.dto.InviteStatus;
import com.clientInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ChannelInviteDAO {
    private static final Logger logger = LoggerFactory.getLogger(ChannelInviteDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public void upsertInvite(ChannelInviteDTO invite) {
        String sql = "MERGE INTO channel_invites (invite_id, channel_id, inviter_user_id, invited_user_id, status, created_at) KEY(invite_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, invite.getInviteId());
            stmt.setString(2, invite.getChannelId());
            stmt.setString(3, invite.getInviterUserId());
            stmt.setString(4, invite.getInvitedUserId());
            stmt.setString(5, invite.getStatus().name());
            stmt.setTimestamp(6,
                    Timestamp.valueOf(invite.getCreatedAt() != null ? invite.getCreatedAt() : LocalDateTime.now()));
            stmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error upsert invitacion: {}", e.getMessage());
        }
    }

    public List<ChannelInviteDTO> listPendingForUser(String userId) {
        List<ChannelInviteDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM channel_invites WHERE invited_user_id = ? AND status = 'PENDING' ORDER BY created_at DESC";
        try (Connection conn = connectionManager.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String id = rs.getString("invite_id");
                String channelId = rs.getString("channel_id");
                String inviter = rs.getString("inviter_user_id");
                String invited = rs.getString("invited_user_id");
                InviteStatus status = InviteStatus.valueOf(rs.getString("status"));
                LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();
                list.add(new ChannelInviteDTO(id, channelId, inviter, null, invited, null, status, created));
            }
        } catch (SQLException e) {
            logger.error("Error listando invitaciones: {}", e.getMessage());
        }
        return list;
    }
}
