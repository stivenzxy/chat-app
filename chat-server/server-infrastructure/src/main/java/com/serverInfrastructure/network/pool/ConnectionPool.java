package com.serverInfrastructure.network.pool;

import com.serverInfrastructure.network.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ConnectionPool {
    private final int maxConnections;
    private final List<ClientConnection> available = new ArrayList<>();
    private final List<ClientConnection> inUse = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);

    public ConnectionPool(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public synchronized ClientConnection acquireConnection(Socket socket) {
        if (inUse.size() >= maxConnections && available.isEmpty()) {
            logger.warn("El pool de conexiones está lleno, se alcanzó el máximo de conexiones: {}", maxConnections);
            throw new RuntimeException("Número máximo de conexiones alcanzado");
        }

        ClientConnection connection;
        if (!available.isEmpty()) {
            connection = available.removeFirst();
            connection.reset(socket);
            logger.info("Pool: Reutilizando conexión existente del pool (disponibles: {} → {})",
                available.size() + 1, available.size());
        } else {
            connection = new ClientConnection("temp", socket.getInetAddress().getHostAddress(), socket);
            logger.info("Pool: Creando nueva conexión (total en uso: {})", inUse.size() + 1);
        }

        inUse.add(connection);
        return connection;
    }

    public synchronized void releaseConnection(ClientConnection connection) {
        String connectionId = connection.getId();
        
        try {
            if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                connection.getSocket().close();
            }
        } catch (Exception e) {
            logger.debug("Error cerrando socket: {}", e.getMessage());
        }

        inUse.remove(connection);
        connection.resetForReuse(null);
        available.add(connection);
        
        logger.info("Pool: Conexión [{}] liberada y disponible para reutilización (disponibles: {} → {})",
            connectionId, available.size() - 1, available.size());
    }

    public synchronized int getInUseCount() {
        return inUse.size();
    }

    public synchronized int getMaxConnections() {
        return maxConnections;
    }
    
    public synchronized ClientConnection findConnectionById(String id) {
        return inUse.stream()
                .filter(conn -> id.equals(conn.getId()))
                .findFirst()
                .orElse(null);
    }
    
    public synchronized void forceDisconnect(ClientConnection connection) {
        try {
            if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                connection.getSocket().close();
            }
        } catch (Exception e) {
            logger.debug("Error al desconectar forzosamente: {}", e.getMessage());
        }
    }
}
