package com.serverInfrastructure.network.events;

import com.serverDomain.entities.User;
import com.serverInfrastructure.network.messaging.MessageBroadcaster;
import com.serverInfrastructure.network.pool.ConnectionPool;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.ConnectionListener;
import com.serverInfrastructure.observers.ActiveUserObserver;
import com.chatCommon.protocol.ProtocolParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Base64;

public class UserEventHandler implements ActiveUserObserver {
    private static final Logger logger = LoggerFactory.getLogger(UserEventHandler.class);
    
    private final MessageBroadcaster messageBroadcaster;
    private final ConnectionPool connectionPool;
    private final List<ConnectionListener> listeners;
    private final ProtocolParser protocolParser;

    public UserEventHandler(MessageBroadcaster messageBroadcaster, 
                           ConnectionPool connectionPool,
                           List<ConnectionListener> listeners,
                           ProtocolParser protocolParser) {
        this.messageBroadcaster = messageBroadcaster;
        this.connectionPool = connectionPool;
        this.listeners = listeners;
        this.protocolParser = protocolParser;
    }

    @Override
    public void onUserLoggedIn(User user) {
        logger.info("Usuario logueado: {}", user.getUsername().value());
        
        String photoBase64 = "";
        if (user.getPhotoData() != null && user.getPhotoData().length > 0) {
            photoBase64 = Base64.getEncoder().encodeToString(user.getPhotoData());
        }
        
        String message = protocolParser.encode("USER_CONNECTED", user.getId(), user.getUsername().value(), photoBase64);
        messageBroadcaster.broadcastMessage(message, user.getUsername().value());
    }

    @Override
    public void onUserLoggedOut(User user, boolean isLastSession) {
        logger.info("Sesión cerrada: {} (connectionId: {})", user.getUsername().value(), user.getId());
        
        // Solo notificar USER_DISCONNECTED si era la última sesión del usuario
        if (isLastSession) {
            // Ya no hay más sesiones de este usuario, notificar desconexión total
            logger.info("Usuario completamente desconectado: {}", user.getUsername().value());
            String message = protocolParser.encode("USER_DISCONNECTED", user.getId(), user.getUsername().value());
            messageBroadcaster.broadcastMessage(message, null);

            ClientConnection connection = connectionPool.findConnectionById(user.getId());
            if (connection != null) {
                fireClientDisconnected(connection);
            }
        } else {
            // Aún hay otras sesiones activas, solo log informativo
            logger.info("Usuario {} aún tiene otra(s) sesión(es) activa(s)", 
                    user.getUsername().value());
        }
    }

    private void fireClientDisconnected(ClientConnection connection) {
        for (ConnectionListener listener : listeners) {
            listener.onClientDisconnected(connection);
        }
    }
}