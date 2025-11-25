package com.serverInfrastructure.persistence.dao;

import com.serverDomain.entities.User;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

    public void insert(User user) {
        String sql = "INSERT INTO users (user_id, username, password_hash, email, photo_data, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername().value());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getEmail().value());
            stmt.setBytes(5, user.getPhotoData());
            stmt.setString(6, user.getIpAddress());
            stmt.setTimestamp(7, Timestamp.valueOf(user.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException exception) {
            logger.error("Error al insertar usuario: {}", exception.getMessage());
        }
    }


    public Optional<User> findByUsername(Username username) {
        String sql = "SELECT * FROM users WHERE BINARY username = ?";
        return getUser(username.value(), sql);
    }

    private Optional<User> getUser(String username, String sql) {
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(mapToUser(rs));
            }
        } catch (SQLException exception) {
            logger.error("Error al obtener usuario con nombre: {} : {}", username, exception.getMessage());
        }
        return Optional.empty();
    }

    public void deleteById(String id) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, id);
            stmt.executeUpdate();
        } catch (SQLException exception) {
            logger.error("Error al eliminar usuario con id {}: {}", id, exception.getMessage());
        }
    }

    public List<User> selectAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = mapToUser(rs);
                if (user != null) {
                    users.add(user);
                }
            }
        } catch (SQLException exception) {
            logger.error("Error al obtener todos los usuarios: {}", exception.getMessage());
        }
        return users;
    }

    public Optional<User> findByEmail(Email email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.value());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.ofNullable(mapToUser(rs));
            }
        } catch (SQLException exception) {
            logger.error("Error al obtener usuario por email {}: {}", email.value(), exception.getMessage());
        }
        return Optional.empty();
    }
    
    /**
     * Inserts a replicated user from a remote server.
     */
    public void insertReplicated(User user) {
        String sql = "INSERT INTO users (user_id, username, password_hash, email, photo_data, ip_address, " +
                    "is_replicated, origin_server_id, last_sync_at, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername().value());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getEmail().value());
            stmt.setBytes(5, user.getPhotoData());
            stmt.setString(6, user.getIpAddress());
            stmt.setBoolean(7, user.isReplicated());
            stmt.setString(8, user.getOriginServerId());
            stmt.setTimestamp(9, user.getLastSyncAt());
            stmt.setTimestamp(10, Timestamp.valueOf(user.getCreatedAt()));
            
            stmt.executeUpdate();
            logger.info("Usuario replicado insertado: {} (origen: {})", 
                       user.getUsername().value(), user.getOriginServerId());
            
        } catch (SQLException exception) {
            logger.error("Error al insertar usuario replicado: {}", exception.getMessage());
            throw new RuntimeException("Error insertando usuario replicado", exception);
        }
    }
    
    /**
     * Updates the last_sync_at timestamp for all replicated users from a specific server.
     */
    public void updateReplicatedTimestamp(String originServerId) {
        String sql = "UPDATE users SET last_sync_at = ? WHERE is_replicated = TRUE AND origin_server_id = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setString(2, originServerId);
            int updated = stmt.executeUpdate();
            
            logger.debug("Actualizados {} usuarios replicados del servidor {}", updated, originServerId);
            
        } catch (SQLException exception) {
            logger.error("Error actualizando timestamp de usuarios replicados: {}", exception.getMessage());
        }
    }

    private User mapToUser(ResultSet rs) {
        try {
            User user = new User(
                    rs.getString("user_id"),
                    new Username(rs.getString("username")),
                    new Email(rs.getString("email")),
                    rs.getString("password_hash"),
                    rs.getBytes("photo_data"),
                    rs.getString("ip_address"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
            
            // Map replication fields if they exist
            try {
                user.setReplicated(rs.getBoolean("is_replicated"));
                user.setOriginServerId(rs.getString("origin_server_id"));
                Timestamp lastSync = rs.getTimestamp("last_sync_at");
                if (lastSync != null) {
                    user.setLastSyncAt(lastSync);
                }
            } catch (SQLException e) {
                // Columns might not exist in older schema, ignore
            }
            
            return user;
        } catch (SQLException exception) {
            logger.error("Error al mapear ResultSet a User: {}", exception.getMessage());
            return null;
        }
    }
}