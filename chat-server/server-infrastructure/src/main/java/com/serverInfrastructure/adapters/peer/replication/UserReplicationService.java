package com.serverInfrastructure.adapters.peer.replication;

import com.chatCommon.dto.ReplicatedUserDTO;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.valueObjects.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;


public class UserReplicationService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserReplicationService.class);
    
    private final UserRepository userRepository;
    private Consumer<Void> onUserReplicationCallback;
    
    public UserReplicationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    public void setOnUserReplicationCallback(Consumer<Void> callback) {
        this.onUserReplicationCallback = callback;
    }
    
    public List<ReplicatedUserDTO> getLocalUsersForReplication(String localServerId) {
        try {
            List<User> allUsers = userRepository.findAll();
            List<ReplicatedUserDTO> replicableUsers = new ArrayList<>();
            
            for (User user : allUsers) {
                if (!user.isReplicated()) {
                    String photoBase64 = encodePhoto(user.getPhotoData());
                    
                    replicableUsers.add(new ReplicatedUserDTO(
                        user.getId(),
                        user.getUsername().value(),
                        user.getEmail().value(),
                        user.getPasswordHash(),
                        photoBase64,
                        localServerId
                    ));
                }
            }
            
            logger.info("Preparados {} usuarios locales para replicación", replicableUsers.size());
            return replicableUsers;
            
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios locales para replicación: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public ReplicationResult replicateUsers(List<ReplicatedUserDTO> remoteUsers) {
        int inserted = 0;
        int skipped = 0;
        int errors = 0;
        
        for (ReplicatedUserDTO dto : remoteUsers) {
            try {
                if (userRepository.findByUsername(new Username(dto.getUsername())).isPresent()) {
                    logger.debug("Usuario {} ya existe localmente, omitiendo replicación", dto.getUsername());
                    skipped++;
                    continue;
                }
                
                User replicatedUser = new User(
                    dto.getUserId(),
                    dto.getUsername(),
                    dto.getEmail(),
                    dto.getPasswordHash()
                );
                
                replicatedUser.setPhotoData(dto.getPhotoData());
                replicatedUser.setReplicated(true);
                replicatedUser.setOriginServerId(dto.getOriginServerId());
                replicatedUser.setLastSyncAt(new Timestamp(System.currentTimeMillis()));
                
                userRepository.saveReplicatedUser(replicatedUser);
                inserted++;
                
                logger.info("Usuario replicado: {} (origen: {})", dto.getUsername(), dto.getOriginServerId());
                
            } catch (Exception e) {
                logger.error("Error replicando usuario {}: {}", dto.getUsername(), e.getMessage());
                errors++;
            }
        }
        
        logger.info("Replicación completada: {} insertados, {} omitidos, {} errores", 
                   inserted, skipped, errors);
        
        if (inserted > 0 && onUserReplicationCallback != null) {
            try {
                onUserReplicationCallback.accept(null);
                logger.debug("Notificación de replicación enviada a UI");
            } catch (Exception e) {
                logger.warn("Error notificando replicación a UI: {}", e.getMessage());
            }
        }
        
        return new ReplicationResult(inserted, skipped, errors);
    }
    
    public void updateSyncTimestamp(String originServerId) {
        try {
            userRepository.updateReplicatedUsersTimestamp(originServerId);
            logger.debug("Timestamp de sincronización actualizado para servidor {}", originServerId);
        } catch (Exception e) {
            logger.warn("Error actualizando timestamp de sincronización: {}", e.getMessage());
        }
    }
    
    private String encodePhoto(byte[] photoData) {
        if (photoData != null && photoData.length > 0) {
            return java.util.Base64.getEncoder().encodeToString(photoData);
        }
        return "";
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
