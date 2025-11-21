package com.serverInfrastructure.adapters.peer.replication;

import com.chatCommon.dto.ReplicatedPeerDTO;
import com.serverDomain.entities.Peer;
import com.serverDomain.repositories.PeerRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PeerReplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerReplicationService.class);
    
    private final PeerRegistryRepository peerRepository;
    
    public PeerReplicationService(PeerRegistryRepository peerRepository) {
        this.peerRepository = peerRepository;
    }
    
    public List<ReplicatedPeerDTO> getActivePeersForReplication() {
        try {
            List<Peer> activePeers = peerRepository.findAllActive();
            List<ReplicatedPeerDTO> replicablePeers = new ArrayList<>();
            
            for (Peer peer : activePeers) {
                replicablePeers.add(new ReplicatedPeerDTO(
                    peer.getPeerId(),
                    peer.getIpAddress(),
                    peer.getPort(),
                    peer.isActive()
                ));
            }
            
            logger.info("Preparados {} peers activos para replicación", replicablePeers.size());
            return replicablePeers;
            
        } catch (Exception e) {
            logger.error("Error obteniendo peers activos para replicación: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    

    public ReplicationResult replicatePeers(List<ReplicatedPeerDTO> remotePeers, String discoveredFrom) {
        int inserted = 0;
        int skipped = 0;
        int errors = 0;
        
        for (ReplicatedPeerDTO dto : remotePeers) {
            try {
                Optional<Peer> existing = peerRepository.findById(dto.getPeerId());
                
                if (existing.isPresent()) {
                    Peer peer = existing.get();
                    peer.setActive(dto.isActive());
                    peer.updateLastSeen();
                    peerRepository.saveOrUpdate(peer);
                    skipped++;
                    logger.debug("Peer {} ya existe, actualizado", dto.getPeerId());
                } else {
                    Peer newPeer = new Peer(
                        dto.getPeerId(),
                        dto.getIpAddress(),
                        dto.getPort()
                    );
                    newPeer.setActive(dto.isActive());
                    newPeer.setDiscoveredFrom(discoveredFrom);
                    
                    peerRepository.save(newPeer);
                    inserted++;
                    logger.info("Peer descubierto transitivamente: {} (desde: {})", 
                               dto.getPeerId(), discoveredFrom);
                }
                
            } catch (Exception e) {
                logger.error("Error replicando peer {}: {}", dto.getPeerId(), e.getMessage());
                errors++;
            }
        }
        
        logger.info("Replicación de peers completada: {} nuevos, {} actualizados, {} errores", 
                   inserted, skipped, errors);
        
        return new ReplicationResult(inserted, skipped, errors);
    }
    

    public void markPeerActive(String peerId) {
        try {
            peerRepository.markActive(peerId);
            logger.debug("Peer {} marcado como activo", peerId);
        } catch (Exception e) {
            logger.warn("Error marcando peer {} como activo: {}", peerId, e.getMessage());
        }
    }
    

    public void markPeerInactive(String peerId) {
        try {
            peerRepository.markInactive(peerId);
            logger.debug("Peer {} marcado como inactivo", peerId);
        } catch (Exception e) {
            logger.warn("Error marcando peer {} como inactivo: {}", peerId, e.getMessage());
        }
    }
    
    public static class ReplicationResult {
        private final int inserted;
        private final int skipped;
        private final int errors;
        
        public ReplicationResult(int inserted, int skipped, int errors) {
            this.inserted = inserted;
            this.skipped = skipped;
            this.errors = errors;
        }
        
        public int getInserted() {
            return inserted;
        }
        
        public int getSkipped() {
            return skipped;
        }
        
        public int getErrors() {
            return errors;
        }
        
        public boolean hasErrors() {
            return errors > 0;
        }
        
        public int getTotal() {
            return inserted + skipped + errors;
        }
    }
}
