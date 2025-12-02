package com.serverInfrastructure.adapters.peer.user;

import com.serverDomain.entities.User;
import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LocalUserRepository {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalUserRepository.class);
    
    public List<String> getLocalUsernames() {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            return new ArrayList<>(userManager.getAllUserSessions().keySet());
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios locales: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
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
    
    public List<User> getUserSessions(String username) {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            return userManager.getUserSessions(username);
        } catch (Exception e) {
            logger.error("Error obteniendo sesiones de usuario {}: {}", username, e.getMessage());
            return new ArrayList<>();
        }
    }
    
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
