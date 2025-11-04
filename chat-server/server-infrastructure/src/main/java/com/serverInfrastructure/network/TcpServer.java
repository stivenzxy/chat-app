package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.handlers.ClientHandler;
import com.serverInfrastructure.network.lifecycle.ServerShutdownManager;
import com.serverInfrastructure.network.messaging.MessageBroadcaster;
import com.serverInfrastructure.network.events.UserEventHandler;
import com.serverInfrastructure.network.pool.ConnectionPool;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.services.CommandHandler;
import com.chatCommon.utils.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpServer.class);
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    private final int port;
    private final CommandHandler commandHandler;
    private final ProtocolParser protocolParser;
    private volatile ServerSocket serverSocket;

    private final ConnectionPool connectionPool;
    private final MessageBroadcaster messageBroadcaster;
    private final ServerShutdownManager shutdownManager;
    private final UserEventHandler userEventHandler;

    private final List<ConnectionListener> listeners = new CopyOnWriteArrayList<>();

    public TcpServer(int port, CommandHandler commandHandler) {
        this.port = port;
        this.commandHandler = commandHandler;
        this.commandHandler.setServer(this);
        this.protocolParser = new ProtocolParser('|', '\\');

        AppProperties props = new AppProperties("server-configuration");
        int maxConnections = props.getInt("MAX_CONNECTIONS");
        this.connectionPool = new ConnectionPool(maxConnections);

        this.messageBroadcaster = new MessageBroadcaster(connectionPool, protocolParser);
        this.shutdownManager = new ServerShutdownManager(connectionPool, protocolParser);
        this.userEventHandler = new UserEventHandler(messageBroadcaster, connectionPool, listeners, protocolParser);

        ActiveUserManager.getInstance().addObserver(userEventHandler);
    }

    public void start() {
        try {
            this.serverSocket = new ServerSocket(port);
            logger.info("Servidor TCP iniciado y escuchando en el puerto {}", port);

            while (!serverSocket.isClosed()) {
                acceptNewConnection();
            }
        } catch (SocketException exception) {
            handleSocketException(exception);
        } catch (IOException exception) {
            logger.error("Error al iniciar el servidor", exception);
            throw new RuntimeException(exception);
        }
    }

    private void acceptNewConnection() throws IOException {
        Socket socket = serverSocket.accept();
        ClientConnection connection;

        try {
            connection = connectionPool.acquireConnection(socket);
            connection.setId("cliente-" + connectionCounter.incrementAndGet());

            fireClientConnected(connection);

            ClientHandler clientHandler = new ClientHandler(
                connection, commandHandler, protocolParser, connectionPool, listeners);
            Thread clientThread = new Thread(clientHandler);
            clientThread.setName(connection.getId());
            clientThread.start();

        } catch (RuntimeException exception) {
            handleMaxConnectionsReached(socket);
        }
    }

    private void handleMaxConnectionsReached(Socket socket) {
        logger.warn("Conexión rechazada: se alcanzó el máximo de usuarios permitidos");

        try (PrintWriter tempOut = new PrintWriter(socket.getOutputStream(), true)) {
            String rejectMessage = protocolParser.encode("DISCONNECT", "Servidor a máxima capacidad. Intente más tarde.");
            tempOut.println(rejectMessage);
        } catch (Exception ignored) {}

        try {
            socket.close();
        } catch (Exception ignored) {}
    }

    private void handleSocketException(SocketException exception) {
        if (serverSocket != null && serverSocket.isClosed()) {
            logger.info("Servidor detenido correctamente");
        } else {
            logger.error("Error de socket inesperado", exception);
            throw new RuntimeException(exception);
        }
    }

    public void stop() {
        shutdownManager.shutdownServer(serverSocket);
    }

    public void addConnectionListener(ConnectionListener listener) {
        this.listeners.add(listener);
    }

    public int getMaxConnections() {
        return connectionPool != null ? connectionPool.getMaxConnections() : 0;
    }

    public int getCurrentConnections() {
        return connectionPool != null ? connectionPool.getInUseCount() : 0;
    }

    public void disconnectClient(String clientId) {
        if (connectionPool != null) {
            messageBroadcaster.notifyClientDisconnection(clientId);
        }
    }

    public boolean sendMessageToUser(String username, String message, String senderInfo) {
        return messageBroadcaster.sendMessageToUser(username, message, senderInfo);
    }
    
    public boolean sendMessageToUserExceptSession(String username, String message, String excludeConnectionId, String senderInfo) {
        return messageBroadcaster.sendMessageToUserExceptSession(username, message, excludeConnectionId, senderInfo);
    }

    public void sendBroadcastMessage(String message) {
        if (messageBroadcaster != null) {
            messageBroadcaster.broadcastMessage(message, null);
        }
    }

    public void fireClientIdentityUpdatedPublic(ClientConnection connection) {
        fireClientIdentityUpdated(connection);
    }


    private void fireClientConnected(ClientConnection connection) {
        for (ConnectionListener listener : listeners) {
            listener.onClientConnected(connection);
        }
    }

    private void fireClientIdentityUpdated(ClientConnection connection) {
        for (ConnectionListener listener : listeners) {
            listener.onClientIdentityUpdated(connection);
        }
    }
}