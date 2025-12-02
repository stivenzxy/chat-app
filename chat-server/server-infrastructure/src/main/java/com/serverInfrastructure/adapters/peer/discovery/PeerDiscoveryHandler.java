package com.serverInfrastructure.adapters.peer.discovery;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.peer.connection.PeerConnectionValidator;
import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import java.util.function.BiConsumer;

public class PeerDiscoveryHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerDiscoveryHandler.class);
    private static final ProtocolParser parser = new ProtocolParser('|', '\\');
    
    private final PeerConnectionValidator validator;
    private BiConsumer<String, Integer> connectionCallback;
    private int localServerPort = -1;
    
    public PeerDiscoveryHandler(PeerConnectionValidator validator) {
        this.validator = validator;
    }
    
    public void setConnectionCallback(BiConsumer<String, Integer> callback) {
        this.connectionCallback = callback;
    }
    
    public void setLocalServerPort(int port) {
        this.localServerPort = port;
    }
    
    public void processPeerList(String sourcePeerId, String peerListMessage) {
        try {
            if (peerListMessage == null || peerListMessage.isBlank()) return;
            
            List<String> parts = parser.decode(peerListMessage);
            if (parts.size() < 2) {
                logger.debug("Formato inválido de peer list: {}", peerListMessage);
                return;
            }
            
            String peersSection = parts.get(1);
            if (peersSection.startsWith("peers=")) {
                peersSection = peersSection.substring(6);
            }
            String[] peerEntries = peersSection.split(",");
            
            for (String entry : peerEntries) {
                String peerId = entry.trim();
                if (peerId.isEmpty()) continue;
                if (!PeerIdParser.isValidFormat(peerId)) continue;
                if (peerId.equals(sourcePeerId)) continue;
                
                String ip = PeerIdParser.extractIp(peerId);
                int port = PeerIdParser.extractPort(peerId);
                
                if (localServerPort != -1 && validator.isLocalAddress(ip, port, localServerPort)) continue;
                
                if (connectionCallback != null) {
                    new Thread(() -> {
                        try {
                            logger.info("Descubierto peer {}, intentando conectar...", peerId);
                            connectionCallback.accept(ip, port);
                        } catch (Exception ex) {
                            logger.warn("Error intentando conectar peer descubierto {}: {}", peerId, ex.getMessage());
                        }
                    }, "PeerAutoConnect-" + peerId).start();
                }
            }
            
        } catch (Exception e) {
            logger.warn("Error procesando peerList de {}: {}", sourcePeerId, e.getMessage());
        }
    }
}
