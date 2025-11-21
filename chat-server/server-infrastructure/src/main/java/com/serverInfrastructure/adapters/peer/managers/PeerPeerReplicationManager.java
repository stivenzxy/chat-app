package com.serverInfrastructure.adapters.peer.managers;

import com.chatCommon.dto.ReplicatedPeerDTO;
import com.serverDomain.repositories.PeerRegistryRepository;
import com.serverInfrastructure.adapters.peer.replication.PeerReplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manager responsible for coordinating peer discovery replication across P2P servers.
 * Implements transitive peer discovery where peers share their known peers.
 * 
 * SRP: Manages only peer replication coordination between P2P servers
 */
public class PeerPeerReplicationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerPeerReplicationManager.class);
    
    private final PeerReplicationService replicationService;
    private String localServerId;
    private BiConsumer<String, String> peerMessageSender;
    private Consumer<String> autoConnectCallback;
    
    public PeerPeerReplicationManager(PeerRegistryRepository peerRepository) {
        this.replicationService = new PeerReplicationService(peerRepository);
    }
    
    public void setLocalServerId(String serverId) {
        this.localServerId = serverId;
    }
    
    public void setPeerMessageSender(BiConsumer<String, String> sender) {
        this.peerMessageSender = sender;
    }
    
    /**
     * Sets callback for auto-connecting to newly discovered peers.
     * This enables transitive peer discovery.
     */
    public void setAutoConnectCallback(Consumer<String> callback) {
        this.autoConnectCallback = callback;
    }
    
    /**
     * Sends all known active peers to a newly connected peer for transitive discovery.
     */
    public void sendPeersToPeer(String targetPeerId) {
        if (localServerId == null) {
            logger.warn("LocalServerId no configurado, no se pueden replicar peers");
            return;
        }
        
        try {
            List<ReplicatedPeerDTO> activePeers = replicationService.getActivePeersForReplication();
            
            if (activePeers.isEmpty()) {
                logger.info("No hay peers activos para compartir con {}", targetPeerId);
                return;
            }
            
            String batchMessage = ReplicatedPeerDTO.toBatchProtocol(activePeers, localServerId);
            
            if (peerMessageSender != null) {
                peerMessageSender.accept(targetPeerId, batchMessage);
                logger.info("Enviados {} peers para descubrimiento transitivo a {}", 
                           activePeers.size(), targetPeerId);
            } else {
                logger.warn("PeerMessageSender no configurado, no se pueden enviar peers");
            }
            
        } catch (Exception e) {
            logger.error("Error enviando peers a {}: {}", targetPeerId, e.getMessage());
        }
    }
    
    /**
     * Handles incoming peer discovery batch from a remote peer.
     * Implements transitive discovery by storing and auto-connecting to new peers.
     */
    public void handleIncomingPeerReplication(String sourcePeerId, String message) {
        try {
            List<ReplicatedPeerDTO> remotePeers = ReplicatedPeerDTO.fromBatchProtocol(message);
            
            logger.info("Recibidos {} peers para descubrimiento desde {}", 
                       remotePeers.size(), sourcePeerId);
            
            PeerReplicationService.ReplicationResult result = 
                replicationService.replicatePeers(remotePeers, sourcePeerId);
            
            logger.info("Descubrimiento desde {}: {} nuevos, {} actualizados, {} errores",
                       sourcePeerId, result.getInserted(), result.getSkipped(), result.getErrors());
            
            // Auto-connect to newly discovered peers (transitive discovery)
            if (result.getInserted() > 0 && autoConnectCallback != null) {
                for (ReplicatedPeerDTO peer : remotePeers) {
                    // Only auto-connect to new peers (not updated ones)
                    if (peer.isActive() && !peer.getPeerId().equals(localServerId)) {
                        logger.info("Iniciando auto-conexión a peer descubierto transitivamente: {}", 
                                   peer.getPeerId());
                        autoConnectCallback.accept(peer.getPeerId());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("Error procesando replicación de peers desde {}: {}", 
                        sourcePeerId, e.getMessage());
        }
    }
    
    /**
     * Marks a peer as active when it successfully connects.
     */
    public void markPeerActive(String peerId) {
        replicationService.markPeerActive(peerId);
    }
    
    /**
     * Marks a peer as inactive when it disconnects.
     */
    public void markPeerInactive(String peerId) {
        replicationService.markPeerInactive(peerId);
    }
}
