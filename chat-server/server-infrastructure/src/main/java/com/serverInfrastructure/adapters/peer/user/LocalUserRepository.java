package com.serverInfrastructure.adapters.peer.user;

import com.serverDomain.entities.User;
import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Repository abstraction for accessing local user information.
 * Decouples peer components from direct ActiveUserManager dependency.
 */
public class LocalUserRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalUserRepository.class);
    
    /**
     * Gets list of all local usernames.
     */
    public List<String> getLocalUsernames() {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            return new ArrayList<>(userManager.getAllUserSessions().keySet());
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios locales: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets local users with their photos in base64 format.
     */
    public Map<String, String> getLocalUsersWithPhotos() {
        Map<String, String> usersWithPhotos = new HashMap<>();
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            Map<String, List<User>> allSessions = userManager.getAllUserSessions();

            for (Map.Entry<String, List<User>> entry : allSessions.entrySet()) {
                String username = entry.getKey();
                List<User> sessions = entry.getValue();

                if (!sessions.isEmpty()) {
                    byte[] photoData = sessions.get(0).getPhotoData();
                    String photoBase64 = encodePhoto(photoData);
                    usersWithPhotos.put(username, photoBase64);
                }
            }
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios con fotos: {}", e.getMessage(), e);
        }
        return usersWithPhotos;
    }
    
    /**
     * Gets photo for a specific user.
     */
    public String getUserPhoto(String username) {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            List<User> sessions = userManager.getUserSessions(username);

            if (!sessions.isEmpty()) {
                byte[] photoData = sessions.get(0).getPhotoData();
                return encodePhoto(photoData);
            }
        } catch (Exception e) {
            logger.error("Error obteniendo foto de usuario {}: {}", username, e.getMessage());
        }
        return "";
    }
    
    /**
     * Gets user sessions for a username.
     */
    public List<User> getUserSessions(String username) {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            return userManager.getUserSessions(username);
        } catch (Exception e) {
            logger.error("Error obteniendo sesiones de usuario {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Gets user ID from connection ID.
     */
    public String getUserIdFromConnection(String connectionId) {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            return userManager.getUserIdFromConnection(connectionId);
        } catch (Exception e) {
            logger.error("Error obteniendo userId desde connectionId {}: {}", connectionId, e.getMessage());
            return null;
        }
    }
    
    private String encodePhoto(byte[] photoData) {
        if (photoData != null && photoData.length > 0) {
            return Base64.getEncoder().encodeToString(photoData);
        }
        return "";
    }
}
