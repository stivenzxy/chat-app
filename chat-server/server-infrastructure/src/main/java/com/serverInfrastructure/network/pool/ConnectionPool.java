package com.serverInfrastructure.network.pool;

import com.serverInfrastructure.network.ClientConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

public class ConnectionPool {
    private final int maxConnections;
    private final List<ClientConnection> available = new ArrayList<>();
    private final List<ClientConnection> inUse = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(ConnectionPool.class);
    private long sequenceGenerator = 0L;

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
            connection.incrementReuseCount();
            logger.info("Pool: Reutilizando conexión instanciaSeq={} reuseCount={} (disponibles: {} → {})",
                connection.getPoolSequence(), connection.getReuseCount(), available.size() + 1, available.size());
        } else {
            connection = new ClientConnection("temp", socket.getInetAddress().getHostAddress(), socket);
            connection.setPoolSequence(++sequenceGenerator);
            logger.info("Pool: Creando nueva conexión instanciaSeq={} (total en uso: {})", connection.getPoolSequence(), inUse.size() + 1);
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
        
        logger.info("Pool: Conexión [{}] instanciaSeq={} liberada -> reuseCount actual {} (disponibles: {} → {})",
            connectionId, connection.getPoolSequence(), connection.getReuseCount(), available.size() - 1, available.size());
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

    public synchronized List<ClientConnection> getInUseConnections() {
        return Collections.unmodifiableList(new ArrayList<>(inUse));
    }
}
