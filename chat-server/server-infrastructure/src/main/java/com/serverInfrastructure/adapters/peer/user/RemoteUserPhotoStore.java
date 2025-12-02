package com.serverInfrastructure.adapters.peer.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteUserPhotoStore {
    
    private static final Logger logger = LoggerFactory.getLogger(RemoteUserPhotoStore.class);
    
    private final Map<String, String> photoCache = new ConcurrentHashMap<>();

    public void storePhoto(String username, String photoBase64) {
        if (username != null && photoBase64 != null) {
            photoCache.put(username, photoBase64);
            logger.debug("Foto almacenada para usuario remoto: {}", username);
        }
    }
    
    public void storePhotos(Map<String, String> photos) {
        if (photos != null && !photos.isEmpty()) {
            photoCache.putAll(photos);
            logger.debug("Almacenadas {} fotos de usuarios remotos", photos.size());
        }
    }

    public String getPhoto(String username) {
        return photoCache.getOrDefault(username, "");
    }

    public void removePhoto(String username) {
        photoCache.remove(username);
        logger.debug("Foto removida para usuario: {}", username);
    }

    public void clear() {
        photoCache.clear();
        logger.debug("Cache de fotos limpiado");
    }
}
