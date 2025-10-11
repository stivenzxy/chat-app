package com.serverInfrastructure.persistence.dao;

import com.serverDomain.entity.User;
import com.serverDomain.valueObject.Email;
import com.serverDomain.valueObject.Username;
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
        String sql = "INSERT INTO users (user_id, username, password_hash, email, photo_path, ip_address, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, user.getId());
            stmt.setString(2, user.getUsername().value());
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getEmail().value());
            stmt.setString(5, user.getPhotoUrl());
            stmt.setString(6, user.getIpAddress());
            stmt.setTimestamp(7, Timestamp.valueOf(user.getCreatedAt()));
            stmt.executeUpdate();
        } catch (SQLException exception) {
            logger.error("Error al insertar usuario: {}", exception.getMessage());
        }
    }

    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        return getUser(username, sql);
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

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        return getUser(email, sql);
    }

    public List<User> selectAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT user_id, username, email, photo_url, ip_address FROM users";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                users.add(mapToUser(rs));
            }
        } catch (SQLException exception) {
            logger.error("Error al obtener todos los usuarios: {}", exception.getMessage());
        }
        return users;
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

    private User mapToUser(ResultSet rs) {
        try {
            return new User(
                    rs.getString("user_id"),
                    new Username(rs.getString("username")),
                    new Email(rs.getString("email")),
                    rs.getString("password_hash"),
                    rs.getString("photo_path"),
                    rs.getString("ip_address"),
                    rs.getTimestamp("created_at").toLocalDateTime()
            );
        } catch (SQLException exception) {
            logger.error("Error al insertar el audio: {}", exception.getMessage());
            return null;
        }
    }
}