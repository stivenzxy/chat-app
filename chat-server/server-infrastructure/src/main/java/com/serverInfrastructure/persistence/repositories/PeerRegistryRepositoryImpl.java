package com.serverInfrastructure.persistence.repositories;

import com.serverDomain.entities.Peer;
import com.serverDomain.repositories.PeerRegistryRepository;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import com.serverInfrastructure.persistence.PeerDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class PeerRegistryRepositoryImpl implements PeerRegistryRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerRegistryRepositoryImpl.class);
    
    private final ConnectionManager connectionManager;
    
    public PeerRegistryRepositoryImpl(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    @Override
    public void save(Peer peer) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);

            if (dao.findById(peer.getPeerId()).isPresent()) {
                throw new IllegalArgumentException("Peer con ID " + peer.getPeerId() + " ya existe");
            }
            
            dao.insert(peer);
            logger.info("Peer {} guardado en registry", peer.getPeerId());
            
        } catch (SQLException e) {
            logger.error("Error guardando peer {}: {}", peer.getPeerId(), e.getMessage());
            throw new RuntimeException("Error al guardar peer", e);
        }
    }
    
    @Override
    public void saveOrUpdate(Peer peer) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            dao.upsert(peer);
            logger.info("Peer {} guardado/actualizado en registry", peer.getPeerId());
            
        } catch (SQLException e) {
            logger.error("Error guardando/actualizando peer {}: {}", peer.getPeerId(), e.getMessage());
            throw new RuntimeException("Error al guardar/actualizar peer", e);
        }
    }
    
    @Override
    public Optional<Peer> findById(String peerId) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            return dao.findById(peerId);
            
        } catch (SQLException e) {
            logger.error("Error buscando peer {}: {}", peerId, e.getMessage());
            return Optional.empty();
        }
    }
    
    @Override
    public List<Peer> findAll() {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            return dao.findAll();
            
        } catch (SQLException e) {
            logger.error("Error recuperando todos los peers: {}", e.getMessage());
            throw new RuntimeException("Error al recuperar peers", e);
        }
    }
    
    @Override
    public List<Peer> findAllActive() {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            return dao.findAllActive();
            
        } catch (SQLException e) {
            logger.error("Error recuperando peers activos: {}", e.getMessage());
            throw new RuntimeException("Error al recuperar peers activos", e);
        }
    }
    
    @Override
    public void markActive(String peerId) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            dao.markActive(peerId);
            logger.info("Peer {} marcado como activo", peerId);
            
        } catch (SQLException e) {
            logger.error("Error marcando peer {} como activo: {}", peerId, e.getMessage());
        }
    }
    
    @Override
    public void markInactive(String peerId) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            dao.markInactive(peerId);
            logger.info("Peer {} marcado como inactivo", peerId);
            
        } catch (SQLException e) {
            logger.error("Error marcando peer {} como inactivo: {}", peerId, e.getMessage());
        }
    }

    @Override
    public void delete(String peerId) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            dao.delete(peerId);
            logger.info("Peer {} eliminado de registry", peerId);

        } catch (SQLException e) {
            logger.error("Error eliminando peer {}: {}", peerId, e.getMessage());
            throw new RuntimeException("Error al eliminar peer", e);
        }
    }

    @Override
    public void deleteAll() {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            dao.deleteAll();
            logger.warn("Todos los peers eliminados de registry");
            
        } catch (SQLException e) {
            logger.error("Error eliminando todos los peers: {}", e.getMessage());
            throw new RuntimeException("Error al eliminar todos los peers", e);
        }
    }
    
    @Override
    public int count() {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            return dao.count();
            
        } catch (SQLException e) {
            logger.error("Error contando peers: {}", e.getMessage());
            return 0;
        }
    }
    
    @Override
    public int countActive() {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            return dao.countActive();
            
        } catch (SQLException e) {
            logger.error("Error contando peers activos: {}", e.getMessage());
            return 0;
        }
    }
    
    @Override
    public List<Peer> findByDiscoveredFrom(String sourcePeerId) {
        try (Connection conn = connectionManager.getConnection()) {
            PeerDAO dao = new PeerDAO(conn);
            return dao.findByDiscoveredFrom(sourcePeerId);
            
        } catch (SQLException e) {
            logger.error("Error buscando peers descubiertos desde {}: {}", sourcePeerId, e.getMessage());
            throw new RuntimeException("Error al buscar peers descubiertos", e);
        }
    }
}