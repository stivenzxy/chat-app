package com.serverInfrastructure.adapters.peer.managers;

import com.serverInfrastructure.services.sync.DatabaseSynchronizationService;
import com.serverInfrastructure.services.sync.PresenceReplicationService;
import com.serverApplication.dto.sync.FullDatabaseSyncDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PeerFullSyncManager {
    private static final Logger logger = LoggerFactory.getLogger(PeerFullSyncManager.class);
    private static final long SYNC_REQUEST_COOLDOWN_MS = 5000;

    private final DatabaseSynchronizationService dbSyncService;
    private final PresenceReplicationService presenceService;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> lastSyncRequestTime = new ConcurrentHashMap<>();

    private BiConsumer<String, String> peerMessageSender;

    public PeerFullSyncManager(DatabaseSynchronizationService dbSyncService,
            PresenceReplicationService presenceService) {
        this.dbSyncService = dbSyncService;
        this.presenceService = presenceService;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void setPeerMessageSender(BiConsumer<String, String> peerMessageSender) {
        this.peerMessageSender = peerMessageSender;
    }

    public void setOnSyncCompleteCallback(java.util.function.Consumer<Void> callback) {
        this.dbSyncService.setOnSyncCompleteCallback(callback);
    }

    public void requestFullSync(String peerId) {
        Long lastRequestTime = lastSyncRequestTime.get(peerId);
        long currentTime = System.currentTimeMillis();

        if (lastRequestTime != null && (currentTime - lastRequestTime) < SYNC_REQUEST_COOLDOWN_MS) {
            logger.debug("Ignorando solicitud de full sync a {} - cooldown activo ({}ms restantes)",
                    peerId, SYNC_REQUEST_COOLDOWN_MS - (currentTime - lastRequestTime));
            return;
        }

        if (peerMessageSender != null) {
            peerMessageSender.accept(peerId, "P2P_FULL_SYNC_REQUEST");
            lastSyncRequestTime.put(peerId, currentTime);
            logger.info("Solicitada sincronización completa a peer {}", peerId);
        }
    }

    public void handleFullSyncRequest(String peerId) {
        sendFullDatabaseToPeer(peerId);
    }

    public void sendFullDatabaseToPeer(String peerId) {
        try {
            FullDatabaseSyncDTO data = dbSyncService.exportFullDatabase();
            String json = objectMapper.writeValueAsString(data);
            String base64Data = java.util.Base64.getEncoder().encodeToString(json.getBytes());

            if (peerMessageSender != null) {
                peerMessageSender.accept(peerId, "P2P_FULL_SYNC_RESPONSE|" + base64Data);
                logger.info("Enviada sincronización completa de BD a peer {}", peerId);
            }
        } catch (Exception e) {
            logger.error("Error enviando BD completa a {}: {}", peerId, e.getMessage());
        }
    }

    public void handleFullSyncResponse(String peerId, String message) {
        try {
            String[] parts = message.split("\\|", 2);
            if (parts.length < 2)
                return;

            String base64Data = parts[1];
            String json = new String(java.util.Base64.getDecoder().decode(base64Data));

            FullDatabaseSyncDTO data = objectMapper.readValue(json, FullDatabaseSyncDTO.class);
            dbSyncService.importFullDatabase(data);

            logger.info("Sincronización completa finalizada con peer {}", peerId);
        } catch (Exception e) {
            logger.error("Error procesando respuesta de full sync de {}: {}", peerId, e.getMessage());
        }
    }

    public void handleUserStatusUpdate(String peerId, String message) {
        String[] parts = message.split("\\|");
        if (parts.length < 3)
            return;

        String username = parts[1];
        boolean isOnline = Boolean.parseBoolean(parts[2]);

        presenceService.handleUserStatusUpdate(username, isOnline);
    }

    public void broadcastUserStatus(String username, boolean isOnline) {
        presenceService.broadcastUserStatus(username, isOnline);
    }
}
