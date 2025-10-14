package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.pool.ConnectionPool;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.utils.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServer {
    private final int port;
    private final CommandHandler commandHandler;
    private final ProtocolParser protocolParser;

    private volatile ServerSocket serverSocket;

    private final List<ConnectionListener> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    private final ConnectionPool connectionPool;

    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);

    public TcpServer(int port, CommandHandler commandHandler) {
        this.port = port;
        this.commandHandler = commandHandler;
        this.protocolParser = new ProtocolParser('|', '\\');
        int maxConnections = AppProperties.getInt("MAX_CONNECTIONS", 2);
        this.connectionPool = new ConnectionPool(maxConnections);
    }

    public void start() {
        try {
            this.serverSocket = new ServerSocket(port);
            logger.info("Servidor TCP iniciado y escuchando en el puerto {}", port);

            while (!serverSocket.isClosed()) {
                ClientConnection connection;
                Socket socket = serverSocket.accept();

                try {
                    connection = connectionPool.acquireConnection(socket);
                    connection.setId("cliente-" + connectionCounter.incrementAndGet());

                    fireClientConnected(connection);

                    Thread clientThread = new Thread(() -> handleClient(connection));
                    clientThread.setName(connection.getId());
                    clientThread.start();
                } catch (RuntimeException exception) {
                    logger.warn("Conexión rechazada: se alcanzó el máximo de usuarios permitidos");
                }
            }
        } catch (IOException exception) {
            logger.error("Error al iniciar el servidor", exception);
            throw new RuntimeException(exception);
        }
    }

    public void addConnectionListener(ConnectionListener listener) {
        this.listeners.add(listener);
    }

    private void fireClientConnected(ClientConnection connection) {
        for (ConnectionListener listener : listeners) {
            listener.onClientConnected(connection);
        }
    }

    private void fireClientDisconnected(ClientConnection connection) {
        for (ConnectionListener listener : listeners) {
            listener.onClientDisconnected(connection);
        }
    }

    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException exception) {
            logger.error("Error al cerrar el ServerSocket: {}", exception.getMessage());
        }
    }

    private void handleClient(ClientConnection connection) {
        Socket socket = connection.getSocket();

        logger.info("[{}] Nueva conexión desde {}", connection.getId(), socket.getInetAddress());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String request;

            while ((request = in.readLine()) != null) {
                logger.info("[{}] Petición recibida: {}", connection.getId(), request);

                List<String> parts = protocolParser.decode(request);
                String response = commandHandler.process(parts);

                logger.info("[{}] Enviando respuesta: {}", connection.getId(), response);
                out.println(response);
            }

            logger.info("[{}] Flujo de entrada cerrado por el cliente", connection.getId());
        } catch (IOException exception) {
            logger.error("[{}] Error de comunicación: {}", connection.getId(), exception.getMessage());
        } finally {
            try {
                socket.close();
                connectionPool.releaseConnection(connection);
                fireClientDisconnected(connection);
                logger.info("[{}] Cliente desconectado", connection.getId());
            } catch (IOException e) {
                logger.warn("[{}] Error al cerrar la conexión: {}", connection.getId(), e.getMessage());
            }
        }
    }
}
