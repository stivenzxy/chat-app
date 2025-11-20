package com.serverInfrastructure.adapters.peer.utils;

import com.chatCommon.protocol.ProtocolParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PeerIdParser {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerIdParser.class);
    private static final ProtocolParser parser = new ProtocolParser(':', '\\');

    public static String[] parse(String peerId) {
        if (peerId == null || peerId.isBlank()) {
            return null;
        }
        
        try {
            List<String> parts = parser.decode(peerId);
            if (parts.size() < 2) {
                return null;
            }
            return new String[]{parts.get(0), parts.get(1)};
        } catch (Exception e) {
            logger.debug("Error al parsear el peerId '{}': {}", peerId, e.getMessage());
            return null;
        }
    }
    
    public static String extractIp(String peerId) {
        String[] parts = parse(peerId);
        return parts != null ? parts[0] : null;
    }

    public static int extractPort(String peerId) {
        String[] parts = parse(peerId);
        if (parts == null) {
            return -1;
        }
        
        try {
            return Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            logger.debug("Puerto inválido en el peerId '{}': {}", peerId, e.getMessage());
            return -1;
        }
    }
    
    public static String create(String ip, int port) {
        return parser.encode(ip, String.valueOf(port));
    }

    public static boolean isValidFormat(String peerId) {
        String[] parts = parse(peerId);
        return parts != null && parts.length == 2 && !parts[0].isBlank() && !parts[1].isBlank();
    }
}