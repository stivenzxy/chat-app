package com.serverInfrastructure.adapters.peer.managers;

import com.serverInfrastructure.services.sync.DatabaseSynchronizationService;
import com.serverInfrastructure.services.sync.PresenceReplicationService;
import com.serverApplication.dto.sync.FullDatabaseSyncDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class PeerFullSyncManager {
    private static final Logger logger = LoggerFactory.getLogger(PeerFullSyncManager.class);

    private final DatabaseSynchronizationService dbSyncService;
    private final PresenceReplicationService presenceService;
    private final ObjectMapper objectMapper;

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

    public void requestFullSync(String peerId) {
        if (peerMessageSender != null) {
            peerMessageSender.accept(peerId, "P2P_FULL_SYNC_REQUEST");
            logger.info("Solicitada sincronización completa a peer {}", peerId);
        }
    }

    public void handleFullSyncRequest(String peerId) {
        try {
            FullDatabaseSyncDTO data = dbSyncService.exportFullDatabase();
            String json = objectMapper.writeValueAsString(data);
            // Dividir en chunks si es necesario, pero por ahora asumimos que cabe o el
            // transporte lo maneja
            // El protocolo usa '|' como delimitador, así que debemos escapar o usar otro
            // mecanismo
            // Como es JSON, mejor enviarlo como payload crudo si el protocolo lo permite,
            // o codificarlo en Base64 para evitar conflictos con delimitadores.
            String base64Data = java.util.Base64.getEncoder().encodeToString(json.getBytes());

            if (peerMessageSender != null) {
                peerMessageSender.accept(peerId, "P2P_FULL_SYNC_RESPONSE|" + base64Data);
                logger.info("Enviada respuesta de sincronización completa a peer {}", peerId);
            }
        } catch (Exception e) {
            logger.error("Error procesando solicitud de full sync de {}: {}", peerId, e.getMessage());
        }
    }

    public void handleFullSyncResponse(String peerId, String message) {
        try {
            // P2P_FULL_SYNC_RESPONSE|base64Data
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
        // P2P_USER_STATUS_UPDATE|username|isOnline
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
