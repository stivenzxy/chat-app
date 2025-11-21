package com.serverInfrastructure.persistence;

import com.serverDomain.entities.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for peer registry persistence.
 * Handles all SQL operations for the peer_registry table.
 * 
 * SRP: Manages only peer database operations
 */
public class PeerDAO {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerDAO.class);
    
    private final Connection connection;
    
    public PeerDAO(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Inserts a new peer into the registry.
     */
    public void insert(Peer peer) throws SQLException {
        String sql = "INSERT INTO peer_registry (peer_id, ip_address, port, last_seen_at, " +
                     "is_active, discovered_from, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peer.getPeerId());
            stmt.setString(2, peer.getIpAddress());
            stmt.setInt(3, peer.getPort());
            stmt.setTimestamp(4, peer.getLastSeenAt());
            stmt.setBoolean(5, peer.isActive());
            stmt.setString(6, peer.getDiscoveredFrom());
            stmt.setTimestamp(7, peer.getCreatedAt());
            
            int rows = stmt.executeUpdate();
            logger.debug("Insertado peer {} en registry (rows={})", peer.getPeerId(), rows);
        }
    }
    
    /**
     * Updates an existing peer's metadata.
     */
    public void update(Peer peer) throws SQLException {
        String sql = "UPDATE peer_registry SET ip_address = ?, port = ?, last_seen_at = ?, " +
                     "is_active = ?, discovered_from = ? WHERE peer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peer.getIpAddress());
            stmt.setInt(2, peer.getPort());
            stmt.setTimestamp(3, peer.getLastSeenAt());
            stmt.setBoolean(4, peer.isActive());
            stmt.setString(5, peer.getDiscoveredFrom());
            stmt.setString(6, peer.getPeerId());
            
            int rows = stmt.executeUpdate();
            logger.debug("Actualizado peer {} en registry (rows={})", peer.getPeerId(), rows);
        }
    }
    
    /**
     * Inserts or updates a peer (UPSERT operation).
     */
    public void upsert(Peer peer) throws SQLException {
        String sql = "INSERT INTO peer_registry (peer_id, ip_address, port, last_seen_at, " +
                     "is_active, discovered_from, created_at) VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON DUPLICATE KEY UPDATE ip_address = VALUES(ip_address), " +
                     "port = VALUES(port), last_seen_at = VALUES(last_seen_at), " +
                     "is_active = VALUES(is_active), discovered_from = VALUES(discovered_from)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peer.getPeerId());
            stmt.setString(2, peer.getIpAddress());
            stmt.setInt(3, peer.getPort());
            stmt.setTimestamp(4, peer.getLastSeenAt());
            stmt.setBoolean(5, peer.isActive());
            stmt.setString(6, peer.getDiscoveredFrom());
            stmt.setTimestamp(7, peer.getCreatedAt());
            
            int rows = stmt.executeUpdate();
            logger.debug("Upsert peer {} en registry (rows={})", peer.getPeerId(), rows);
        }
    }
    
    /**
     * Finds a peer by ID.
     */
    public Optional<Peer> findById(String peerId) throws SQLException {
        String sql = "SELECT * FROM peer_registry WHERE peer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapToPeer(rs));
                }
                return Optional.empty();
            }
        }
    }
    
    /**
     * Retrieves all peers.
     */
    public List<Peer> findAll() throws SQLException {
        String sql = "SELECT * FROM peer_registry ORDER BY created_at DESC";
        List<Peer> peers = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                peers.add(mapToPeer(rs));
            }
        }
        
        logger.debug("Recuperados {} peers totales desde registry", peers.size());
        return peers;
    }
    
    /**
     * Retrieves only active peers.
     */
    public List<Peer> findAllActive() throws SQLException {
        String sql = "SELECT * FROM peer_registry WHERE is_active = TRUE ORDER BY last_seen_at DESC";
        List<Peer> peers = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                peers.add(mapToPeer(rs));
            }
        }
        
        logger.debug("Recuperados {} peers activos desde registry", peers.size());
        return peers;
    }
    
    /**
     * Updates last_seen_at timestamp to current time.
     */
    public void updateLastSeen(String peerId) throws SQLException {
        String sql = "UPDATE peer_registry SET last_seen_at = CURRENT_TIMESTAMP WHERE peer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peerId);
            stmt.executeUpdate();
        }
    }
    
    /**
     * Marks a peer as active and updates last seen.
     */
    public void markActive(String peerId) throws SQLException {
        String sql = "UPDATE peer_registry SET is_active = TRUE, last_seen_at = CURRENT_TIMESTAMP " +
                     "WHERE peer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peerId);
            stmt.executeUpdate();
            logger.debug("Peer {} marcado como activo", peerId);
        }
    }
    
    /**
     * Marks a peer as inactive.
     */
    public void markInactive(String peerId) throws SQLException {
        String sql = "UPDATE peer_registry SET is_active = FALSE WHERE peer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peerId);
            stmt.executeUpdate();
            logger.debug("Peer {} marcado como inactivo", peerId);
        }
    }
    
    /**
     * Deletes a peer from the registry.
     */
    public void delete(String peerId) throws SQLException {
        String sql = "DELETE FROM peer_registry WHERE peer_id = ?";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, peerId);
            int rows = stmt.executeUpdate();
            logger.debug("Eliminado peer {} de registry (rows={})", peerId, rows);
        }
    }
    
    /**
     * Deletes all peers.
     */
    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM peer_registry";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            int rows = stmt.executeUpdate();
            logger.warn("Eliminados {} peers de registry (DELETE ALL)", rows);
        }
    }
    
    /**
     * Counts total peers.
     */
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM peer_registry";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Counts active peers.
     */
    public int countActive() throws SQLException {
        String sql = "SELECT COUNT(*) FROM peer_registry WHERE is_active = TRUE";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }
    
    /**
     * Finds peers discovered from a specific source.
     */
    public List<Peer> findByDiscoveredFrom(String sourcePeerId) throws SQLException {
        String sql = "SELECT * FROM peer_registry WHERE discovered_from = ? ORDER BY created_at DESC";
        List<Peer> peers = new ArrayList<>();
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, sourcePeerId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    peers.add(mapToPeer(rs));
                }
            }
        }
        
        return peers;
    }
    
    /**
     * Maps a ResultSet row to a Peer entity.
     */
    private Peer mapToPeer(ResultSet rs) throws SQLException {
        return new Peer(
            rs.getString("peer_id"),
            rs.getString("ip_address"),
            rs.getInt("port"),
            rs.getTimestamp("last_seen_at"),
            rs.getBoolean("is_active"),
            rs.getString("discovered_from"),
            rs.getTimestamp("created_at")
        );
    }
}
