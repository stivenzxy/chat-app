package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.Base64;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.pool.ConnectionPool;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.observers.ActiveUserObserver;
import com.serverDomain.entities.User;
import com.chatCommon.utils.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TcpServer implements ActiveUserObserver {
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
        this.commandHandler.setServer(this);
        this.protocolParser = new ProtocolParser('|', '\\');

        AppProperties props = new AppProperties("server-configuration");
        int maxConnections = props.getInt("MAX_CONNECTIONS");

        this.connectionPool = new ConnectionPool(maxConnections);
        ActiveUserManager.getInstance().addObserver(this);
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

                    try (PrintWriter tempOut = new PrintWriter(socket.getOutputStream(), true)) {
                        String rejectMessage = protocolParser.encode("DISCONNECT", "Servidor a máxima capacidad. Intente más tarde.");
                        tempOut.println(rejectMessage);
                    } catch (Exception ignored) {}
                    try { socket.close(); } catch (Exception ignored) {}
                }
            }
        } catch (SocketException exception) {
            if (serverSocket != null && serverSocket.isClosed()) {
                logger.info("Servidor detenido correctamente");
            } else {
                logger.error("Error de socket inesperado", exception);
                throw new RuntimeException(exception);
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

    private void fireClientIdentityUpdated(ClientConnection connection) {
        for (ConnectionListener listener : listeners) {
            listener.onClientIdentityUpdated(connection);
        }
    }

    public void fireClientIdentityUpdatedPublic(ClientConnection connection) {
        fireClientIdentityUpdated(connection);
    }

    public void stop() {
        logger.info("Iniciando cierre del servidor...");

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

            for (ClientConnection connection : activeConnections) {
                try {
                    connectionPool.forceDisconnect(connection);
                } catch (Exception e) {
                    logger.warn("[{}] Error al cerrar conexión: {}", connection.getId(), e.getMessage());
                }
            }
        }
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            logger.info("Servidor cerrado correctamente");
        } catch (IOException exception) {
            logger.error("Error al cerrar el ServerSocket: {}", exception.getMessage());
        }
    }

    public int getMaxConnections() {
        return connectionPool != null ? connectionPool.getMaxConnections() : 0;
    }

    public int getCurrentConnections() {
        return connectionPool != null ? connectionPool.getInUseCount() : 0;
    }

    public void disconnectClient(String clientId) {
        if (connectionPool != null) {
            ClientConnection connection = connectionPool.findConnectionById(clientId);
            if (connection != null) {

                try {
                    if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                        PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                        String disconnectMessage = protocolParser.encode("DISCONNECT", "Desconectado por el servidor");
                        out.println(disconnectMessage);
                        logger.info("[{}] Notificación de desconexión enviada al cliente", clientId);

                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    logger.warn("[{}] Error al enviar notificación de desconexión: {}", clientId, e.getMessage());
                }

                connectionPool.forceDisconnect(connection);
            } else {
                logger.warn("No se encontró cliente con ID: {}", clientId);
            }
        }
    }

    private void handleClient(ClientConnection connection) {
        Socket socket = connection.getSocket();

        logger.info("[{}] Nueva conexión desde {}", connection.getId(), socket.getInetAddress());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String request;

            while ((request = in.readLine()) != null) {
                List<String> parts = protocolParser.decode(request);

                if (!parts.isEmpty()) {
                    parts.getFirst();
                }

                String response = commandHandler.process(parts, connection);

                logger.info("[{}] Enviando respuesta: {}", connection.getId(), response);
                out.println(response);
            }

            logger.info("[{}] Flujo de entrada cerrado por el cliente", connection.getId());
        } catch (IOException exception) {
            logger.error("[{}] Error de comunicación: {}", connection.getId(), exception.getMessage());
        } finally {
            String connectionId = connection.getId();
            try {
                socket.close();

                String usernameToLogOut = ActiveUserManager.getInstance().getActiveUsers().entrySet().stream()
                        .filter(entry -> entry.getValue().getId().equals(connectionId))
                        .map(java.util.Map.Entry::getKey)
                        .findFirst()
                        .orElse(null);

                if (usernameToLogOut != null) {
                    ActiveUserManager.getInstance().userLoggedOut(usernameToLogOut);
                } else {
                    logger.info("[{}] desconectado antes de completar login. No se notifica.", connectionId);
                }

                fireClientDisconnected(connection);
                connectionPool.releaseConnection(connection);

                logger.info("[{}] Cliente desconectado", connectionId);
            } catch (IOException e) {
                logger.warn("[{}] Error al cerrar la conexión: {}", connectionId, e.getMessage());
            }
        }
    }

    @Override
    public void onUserLoggedIn(User user) {
        // --- INICIO DE LA MODIFICACIÓN ---
        String photoBase64 = "";
        if (user.getPhotoData() != null && user.getPhotoData().length > 0) {
            photoBase64 = Base64.getEncoder().encodeToString(user.getPhotoData());
        }
        String message = protocolParser.encode("USER_CONNECTED", user.getId(), user.getUsername().value(), photoBase64);
        // --- FIN DE LA MODIFICACIÓN ---
        broadcastMessage(message, user.getUsername().value());
    }

    @Override
    public void onUserLoggedOut(User user) {
        String message = protocolParser.encode("USER_DISCONNECTED", user.getId(), user.getUsername().value());
        broadcastMessage(message, null);

        ClientConnection connection = connectionPool.findConnectionById(user.getUsername().value());
        if (connection != null) {
            fireClientDisconnected(connection);
        }
    }

    public boolean sendMessageToUser(String username, String message, String senderInfo) {
        ClientConnection connection = connectionPool.findConnectionByUsername(username);
        if (connection != null) {
            try {
                if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                    PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                    out.println(message);

                    if (senderInfo != null) {
                        String messageContent = extractMessageContent(message);
                        logger.info("Cliente [{}] envió \"{}\" a cliente [{}]",
                                senderInfo, messageContent, username);
                    } else {
                        logger.info("Mensaje directo enviado a [{}]: {}", username, message);
                    }
                    return true;
                }
            } catch (IOException e) {
                logger.warn("[{}] Error al enviar mensaje directo: {}", username, e.getMessage());
            }
        }
        return false;
    }

    private String extractMessageContent(String protocolMessage) {
        try {
            List<String> parts = protocolParser.decode(protocolMessage);
            if (parts.size() >= 3) {
                String command = parts.get(0);
                if ("RECEIVE_PRIVATE_MESSAGE".equals(command)) {
                    return parts.get(2);
                } else if ("RECEIVE_PRIVATE_AUDIO".equals(command)) {
                    return "[Audio message]";
                }
            }
            return protocolMessage;
        } catch (Exception e) {
            return protocolMessage;
        }
    }

    private void broadcastMessage(String message, String excludeUsername) {
        logger.info("Broadcasting: {}", message);
        for (ClientConnection connection : connectionPool.getInUseConnections()) {
            if (connection.getId() != null && !connection.getId().equals(excludeUsername)) {
                try {
                    if (connection.getSocket() != null && !connection.getSocket().isClosed()) {
                        PrintWriter out = new PrintWriter(connection.getSocket().getOutputStream(), true);
                        out.println(message);
                    }
                } catch (IOException e) {
                    logger.warn("[{}] Error al hacer broadcast: {}", connection.getId(), e.getMessage());
                }
            }
        }
    }
}