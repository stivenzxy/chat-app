package com.serverInfrastructure.adapters.peer.lifecycle;

import com.serverInfrastructure.network.peerTcp.PeerTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerLifecycleManager {

    private static final Logger logger = LoggerFactory.getLogger(ServerLifecycleManager.class);

    private PeerTcpServer peerServer;

    public boolean startServer() {
        if (peerServer != null && peerServer.isRunning()) {
            logger.warn("Servidor P2P ya está ejecutándose en puerto {}", peerServer.getPeerServerPort());
            return true;
        }

        try {
            peerServer = new PeerTcpServer();
            peerServer.startPeerServer();

            logger.info("Servidor P2P iniciado exitosamente en puerto {}", peerServer.getPeerServerPort());
            return true;

        } catch (Exception e) {
            logger.error("Error iniciando servidor P2P: {}", e.getMessage());
            peerServer = null;
            return false;
        }
    }

    public void stopServer() {
        if (peerServer != null) {
            logger.info("Deteniendo servidor P2P...");
            peerServer.stopPeerServer();
            peerServer = null;
            logger.info("Servidor P2P detenido");
        }
    }

    public boolean isRunning() {
        return peerServer != null && peerServer.isRunning();
    }

    public PeerTcpServer getServer() {
        return peerServer;
    }

    public int getServerPort() {
        if (peerServer != null) {
            return peerServer.getPeerServerPort();
        }
        return new PeerTcpServer().getPeerServerPort();
    }
}
