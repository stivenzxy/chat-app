package com.serverInfrastructure.adapters.peer.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;

public class LocalServerIdentityProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(LocalServerIdentityProvider.class);
    
    public String resolveLocalServerId(int peerPort) {
        try {
            String localIp = getLocalIp();
            String serverId = localIp + ":" + peerPort;
            logger.info("ID del servidor local: {}", serverId);
            return serverId;
        } catch (Exception e) {
            String serverId = "127.0.0.1:" + peerPort;
            logger.warn("No se pudo obtener IP local, usando: {}", serverId);
            return serverId;
        }
    }
    
    public String getLocalIp() throws Exception {
        return InetAddress.getLocalHost().getHostAddress();
    }
}
