package com.serverInfrastructure.adapters.peer.Managers;

import com.chatCommon.dto.ReplicatedUserDTO;
import com.serverDomain.repositories.UserRepository;
import com.serverInfrastructure.adapters.peer.replication.UserReplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Manager responsible for coordinating user replication across P2P servers.
 * Handles both sending local users and receiving remote users.
 * 
 * SRP: Manages only user replication coordination between peers
 */
public class PeerUserReplicationManager {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerUserReplicationManager.class);
    
    private final UserReplicationService replicationService;
    private String localServerId;
    private BiConsumer<String, String> peerMessageSender;
    
    public PeerUserReplicationManager(UserRepository userRepository) {
        this.replicationService = new UserReplicationService(userRepository);
    }
    
    public void setLocalServerId(String serverId) {
        this.localServerId = serverId;
    }
    
    public void setPeerMessageSender(BiConsumer<String, String> sender) {
        this.peerMessageSender = sender;
    }
    
    /**
     * Sends all local users to a newly connected peer for replication.
     */
    public void sendUsersToPeer(String peerId) {
        if (localServerId == null) {
            logger.warn("LocalServerId no configurado, no se pueden replicar usuarios");
            return;
        }
        
        try {
            List<ReplicatedUserDTO> localUsers = replicationService.getLocalUsersForReplication(localServerId);
            
            if (localUsers.isEmpty()) {
                logger.info("No hay usuarios locales para replicar al peer {}", peerId);
                return;
            }
            
            String batchMessage = ReplicatedUserDTO.toBatchProtocol(localUsers, localServerId);
            
            if (peerMessageSender != null) {
                peerMessageSender.accept(peerId, batchMessage);
                logger.info("Enviados {} usuarios para replicación al peer {}", localUsers.size(), peerId);
            } else {
                logger.warn("PeerMessageSender no configurado, no se pueden enviar usuarios");
            }
            
        } catch (Exception e) {
            logger.error("Error enviando usuarios al peer {}: {}", peerId, e.getMessage());
        }
    }
    
    /**
     * Handles incoming user replication batch from a remote peer.
     */
    public void handleIncomingUserReplication(String sourcePeerId, String message) {
        try {
            List<ReplicatedUserDTO> remoteUsers = ReplicatedUserDTO.fromBatchProtocol(message);
            
            logger.info("Recibidos {} usuarios para replicación desde peer {}", 
                       remoteUsers.size(), sourcePeerId);
            
            UserReplicationService.ReplicationResult result = 
                replicationService.replicateUsers(remoteUsers);
            
            logger.info("Replicación desde {}: {} insertados, {} omitidos, {} errores",
                       sourcePeerId, result.getInserted(), result.getSkipped(), result.getErrors());
            
            if (result.getInserted() > 0 && !remoteUsers.isEmpty()) {
                String originServer = remoteUsers.get(0).getOriginServerId();
                replicationService.updateSyncTimestamp(originServer);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando replicación de usuarios desde {}: {}", 
                        sourcePeerId, e.getMessage());
        }
    }
}
