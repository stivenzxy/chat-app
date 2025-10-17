package com.serverInfrastructure.network.handlers;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.ConnectionListener;
import com.serverInfrastructure.network.pool.ConnectionPool;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.services.CommandHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final ClientConnection connection;
    private final CommandHandler commandHandler;
    private final ProtocolParser protocolParser;
    private final ConnectionPool connectionPool;
    private final List<ConnectionListener> listeners;

    public ClientHandler(ClientConnection connection, 
                        CommandHandler commandHandler, 
                        ProtocolParser protocolParser,
                        ConnectionPool connectionPool,
                        List<ConnectionListener> listeners) {
        this.connection = connection;
        this.commandHandler = commandHandler;
        this.protocolParser = protocolParser;
        this.connectionPool = connectionPool;
        this.listeners = listeners;
    }

    @Override
    public void run() {
        handleClient();
    }

    private void handleClient() {
        Socket socket = connection.getSocket();
        String connectionId = connection.getId();

        logger.info("[{}] Nueva conexión desde {}", connectionId, socket.getInetAddress());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String request;

            while ((request = in.readLine()) != null) {
                List<String> parts = protocolParser.decode(request);
                String response = commandHandler.process(parts, connection);

                String cleanResponse = replaceBase64WithPlaceholder(response);
                logger.info("[{}] Enviando respuesta: {}", connectionId, cleanResponse);
                out.println(response);
            }

            logger.info("[{}] Flujo de entrada cerrado por el cliente", connectionId);
        } catch (IOException exception) {
            logger.error("[{}] Error de comunicación: {}", connectionId, exception.getMessage());
        } finally {
            handleClientDisconnection();
        }
    }

    private void handleClientDisconnection() {
        String connectionId = connection.getId();
        
        try {
            connection.getSocket().close();

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

            fireClientDisconnected();
            connectionPool.releaseConnection(connection);

            logger.info("[{}] Cliente desconectado", connectionId);
        } catch (IOException e) {
            logger.warn("[{}] Error al cerrar la conexión: {}", connectionId, e.getMessage());
        }
    }

    private void fireClientDisconnected() {
        for (ConnectionListener listener : listeners) {
            listener.onClientDisconnected(connection);
        }
    }
    
    private String replaceBase64WithPlaceholder(String message) {
        return message.replaceAll("[A-Za-z0-9+/]{50,}={0,2}", "[foto de perfil]");
    }
}