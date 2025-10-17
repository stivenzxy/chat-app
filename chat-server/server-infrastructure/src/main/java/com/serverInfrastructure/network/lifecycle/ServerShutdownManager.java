package com.serverInfrastructure.network.lifecycle;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.pool.ConnectionPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.util.List;

public class ServerShutdownManager {
    private static final Logger logger = LoggerFactory.getLogger(ServerShutdownManager.class);
    
    private final ConnectionPool connectionPool;
    private final ProtocolParser protocolParser;

    public ServerShutdownManager(ConnectionPool connectionPool, ProtocolParser protocolParser) {
        this.connectionPool = connectionPool;
        this.protocolParser = protocolParser;
    }


    public void shutdownServer(ServerSocket serverSocket) {
        logger.info("Iniciando cierre del servidor...");

        notifyAllClientsOfShutdown();
        closeAllConnections();
        closeServerSocket(serverSocket);
        
        logger.info("Servidor cerrado correctamente");
    }

    private void notifyAllClientsOfShutdown() {
        if (connectionPool != null) {
            List<ClientConnection> activeConnections = connectionPool.getInUseConnections();
            logger.info("Notificando a {} clientes sobre el cierre del servidor", activeConnections.size());
            
            for (ClientConnection connection : activeConnections) {
                try {
                    if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                        PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                        String serverShutdownMessage = protocolParser.encode("SERVER_SHUTDOWN", "El servidor se está cerrando");
                        out.println(serverShutdownMessage);
                        logger.debug("[{}] Notificación de cierre enviada al cliente", connection.getId());
                    }
                } catch (Exception e) {
                    logger.warn("[{}] Error al enviar notificación de cierre: {}", connection.getId(), e.getMessage());
                }
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void closeAllConnections() {
        if (connectionPool != null) {
            List<ClientConnection> activeConnections = connectionPool.getInUseConnections();
            for (ClientConnection connection : activeConnections) {
                try {
                    connectionPool.forceDisconnect(connection);
                } catch (Exception e) {
                    logger.warn("[{}] Error al cerrar conexión: {}", connection.getId(), e.getMessage());
                }
            }
        }
    }

    private void closeServerSocket(ServerSocket serverSocket) {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            logger.error("Error al cerrar el ServerSocket: {}", exception.getMessage());
        }
    }
}