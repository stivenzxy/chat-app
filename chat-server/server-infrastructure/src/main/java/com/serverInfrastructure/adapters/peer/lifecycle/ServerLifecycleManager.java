package com.serverInfrastructure.adapters.peer.lifecycle;

import com.serverInfrastructure.network.peerTcp.PeerTcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle of the P2P server.
 * Handles starting, stopping, and status queries of the PeerTcpServer.
 */
public class ServerLifecycleManager {
    
    private static final Logger logger = LoggerFactory.getLogger(ServerLifecycleManager.class);
    
    private PeerTcpServer peerServer;
    
    /**
     * Starts the P2P server if not already running.
     * 
     * @return true if server started successfully or was already running, false otherwise
     */
    public boolean startServer() {
        if (peerServer != null && peerServer.isRunning()) {
            logger.warn("Servidor P2P ya está ejecutándose en puerto {}", peerServer.getPeerPort());
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
    
    /**
     * Stops the P2P server if it's running.
     */
    public void stopServer() {
        if (peerServer != null) {
            logger.info("Deteniendo servidor P2P...");
            peerServer.stopPeerServer();
            peerServer = null;
            logger.info("Servidor P2P detenido");
        }
    }
    
    /**
     * Checks if the P2P server is currently running.
     * 
     * @return true if the server is running, false otherwise
     */
    public boolean isRunning() {
        return peerServer != null && peerServer.isRunning();
    }
    
    /**
     * Gets the current P2P server instance.
     * 
     * @return the PeerTcpServer instance, or null if not started
     */
    public PeerTcpServer getServer() {
        return peerServer;
    }
    
    /**
     * Gets the port on which the P2P server is listening.
     * If the server is not running, returns the default configured port.
     * 
     * @return the P2P server port
     */
    public int getServerPort() {
        if (peerServer != null) {
            return peerServer.getPeerServerPort();
        }
        return new PeerTcpServer().getPeerServerPort();
    }
}
