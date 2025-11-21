package com.serverInfrastructure.adapters.peer.Managers;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverDomain.entities.Peer;
import com.serverDomain.repositories.PeerRegistryRepository;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import com.serverInfrastructure.persistence.config.ConnectionManager;
import com.serverInfrastructure.persistence.repositories.PeerRegistryRepositoryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PeerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PeerRegistry.class);

    private final PeerRegistryRepository repository;
    private static PeerRegistry instance;

    private PeerRegistry(PeerRegistryRepository repository) {
        this.repository = repository;
        logger.info("PeerRegistry inicializado con persistencia en base de datos");
    }

    public static synchronized PeerRegistry getInstance() {
        if (instance == null) {
            ConnectionManager connManager = ConnectionManager.getInstance();
            PeerRegistryRepository repo = new PeerRegistryRepositoryImpl(connManager);
            instance = new PeerRegistry(repo);
            logger.debug("Instancia de PeerRegistry creada con repository");
        }
        return instance;
    }


    private boolean isPeerPortValid(String peerId) {
        try {
            if (peerId == null || peerId.isBlank()) return false;
            
            int port = PeerIdParser.extractPort(peerId);
            if (port == -1) return false;

            int EPHEMERAL_LOWER = 49152;
            int EPHEMERAL_UPPER = 65535;
            return port < EPHEMERAL_LOWER || port > EPHEMERAL_UPPER;
        } catch (Exception e) {
            logger.debug("Error validando puerto de peer {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    private Peer createPeerFromId(String peerId) {
        String ip = PeerIdParser.extractIp(peerId);
        int port = PeerIdParser.extractPort(peerId);
        return new Peer(peerId, ip, port);
    }

    public synchronized void addKnownPeer(ConnectedPeerInfo peerInfo) {
        if (peerInfo == null) return;
        String id = peerInfo.peerId();
        logger.debug("PeerRegistry.addKnownPeer llamado con ConnectedPeerInfo={}", id);
        
        if (!isPeerPortValid(id)) {
            logger.debug("Ignorando registro de peer con puerto no válido: {}", id);
            return;
        }
        
        try {
            Peer peer = createPeerFromId(id);
            peer.markActive();
            repository.saveOrUpdate(peer);
            logger.info("Peer conocido agregado/actualizado: {}", id);
        } catch (Exception e) {
            logger.error("Error agregando peer {}: {}", id, e.getMessage());
        }
    }

    public synchronized void addKnownPeer(String peerId) {
        if (peerId == null || peerId.isBlank()) return;
        logger.debug("PeerRegistry.addKnownPeer llamado con String={}", peerId);
        
        if (!isPeerPortValid(peerId)) {
            logger.debug("Ignorando registro de peer con puerto no válido (string): {}", peerId);
            return;
        }
        
        try {
            Peer peer = createPeerFromId(peerId);
            peer.markActive();
            repository.saveOrUpdate(peer);
            logger.info("Peer conocido agregado/actualizado (string): {}", peerId);
        } catch (Exception e) {
            logger.error("Error agregando peer nuevo {}: {}", peerId, e.getMessage());
        }
    }

    public synchronized Set<String> getKnownPeers() {
        try {
            return repository.findAll().stream()
                    .map(Peer::getPeerId)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Error recuperando peers conocidos: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    public synchronized Set<String> getValidPeers() {
        try {
            return repository.findAllActive().stream()
                    .map(Peer::getPeerId)
                    .filter(this::isPeerPortValid)
                    .collect(Collectors.toSet());
        } catch (Exception e) {
            logger.error("Error recuperando peers válidos: {}", e.getMessage());
            return new HashSet<>();
        }
    }

    public synchronized void addAll(List<String> peers) {
        if (peers == null || peers.isEmpty()) return;
        
        int added = 0;
        for (String peerId : peers) {
            if (peerId != null && !peerId.isBlank() && isPeerPortValid(peerId)) {
                try {
                    Peer peer = createPeerFromId(peerId);
                    peer.markActive();
                    repository.saveOrUpdate(peer);
                    added++;
                } catch (Exception e) {
                    logger.warn("Error agregando peer {}: {}", peerId, e.getMessage());
                }
            }
        }
        
        logger.debug("PeerRegistry.addAll: {} peers procesados, {} agregados/actualizados", 
                    peers.size(), added);
    }

    public synchronized void clear() {
        try {
            repository.deleteAll();
            logger.warn("Registry de peers limpiado completamente");
        } catch (Exception e) {
            logger.error("Error limpiando registry de peers: {}", e.getMessage());
        }
    }
}
