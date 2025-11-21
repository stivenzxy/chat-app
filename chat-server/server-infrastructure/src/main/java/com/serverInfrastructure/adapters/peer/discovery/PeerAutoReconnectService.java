package com.serverInfrastructure.adapters.peer.discovery;

import com.serverInfrastructure.adapters.peer.managers.PeerRegistry;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class PeerAutoReconnectService {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerAutoReconnectService.class);
    
    private BiConsumer<String, Integer> connectionCallback;

    public void setConnectionCallback(BiConsumer<String, Integer> callback) {
        this.connectionCallback = callback;
    }

    public void reconnectToKnownPeers(String localServerId, Predicate<String> isAlreadyConnected) {
        try {
            PeerRegistry registry = PeerRegistry.getInstance();

            for (String peerId : registry.getKnownPeers()) {

                if (peerId == null || peerId.isBlank()) continue; // Salta ese peerId y continua a la siguiente iteración
                if (peerId.equals(localServerId)) continue;
                if (isAlreadyConnected != null && isAlreadyConnected.test(peerId)) continue;

                attemptReconnection(peerId);
            }

        } catch (Exception e) {
            logger.debug("No se pudo iniciar reconexión a peers conocidos: {}", e.getMessage());
        }
    }


    private void attemptReconnection(String peerId) {
        try {
            if (!PeerIdParser.isValidFormat(peerId)) {
                logger.warn("Formato inválido de peer conocido: {}", peerId);
                return;
            }
            
            String ip = PeerIdParser.extractIp(peerId);
            int port = PeerIdParser.extractPort(peerId);
            
            if (connectionCallback == null) {
                logger.warn("No hay callback configurado para reconectar a {}", peerId);
                return;
            }
            
            logger.info("Reconectando a peer persistido {}", peerId);
            connectionCallback.accept(ip, port);
            
        } catch (Exception ex) {
            logger.warn("No se pudo reconectar peer persistido {}: {}", peerId, ex.getMessage());
        }
    }
}