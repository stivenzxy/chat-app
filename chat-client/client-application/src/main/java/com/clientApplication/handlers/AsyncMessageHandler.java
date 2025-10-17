package com.clientApplication.handlers;

import com.clientApplication.events.*;
import com.clientApplication.listeners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Implementación del handler para procesar mensajes asincrónicos del servidor
 * Aplica principios SOLID:
 * - SRP: Solo se encarga de procesar mensajes asincrónicos
 * - OCP: Abierto para extensión (nuevos tipos de mensaje)
 * - LSP: Implementa correctamente MessageHandler
 * - ISP: Usa interfaces segregadas para listeners
 * - DIP: Depende de abstracciones (interfaces)
 */
public class AsyncMessageHandler implements MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageHandler.class);
    
    // Listas de listeners (permite múltiples observers por tipo)
    private final List<UserConnectionListener> userConnectionListeners = new ArrayList<>();
    private final List<UserDisconnectionListener> userDisconnectionListeners = new ArrayList<>();
    private final List<PrivateMessageListener> privateMessageListeners = new ArrayList<>();
    private final List<PrivateAudioListener> privateAudioListeners = new ArrayList<>();
    
    @Override
    public void handleAsyncMessage(List<String> parts) {
        if (parts == null || parts.isEmpty()) {
            logger.warn("Mensaje asíncrono vacío o nulo recibido");
            return;
        }
        
        String command = parts.getFirst().toUpperCase();
        logger.debug("Procesando comando asíncrono: {}", command);
        
        try {
            switch (command) {
                case "USER_CONNECTED" -> handleUserConnection(parts);
                case "USER_DISCONNECTED" -> handleUserDisconnection(parts);
                case "RECEIVE_PRIVATE_MESSAGE" -> handlePrivateMessage(parts);
                case "RECEIVE_PRIVATE_AUDIO" -> handlePrivateAudio(parts);
                default -> logger.warn("Comando asíncrono desconocido: {}", command);
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje asíncrono [{}]: {}", command, e.getMessage(), e);
        }
    }
    
    private void handleUserConnection(List<String> parts) {
        if (parts.size() < 3) {
            logger.warn("Mensaje USER_CONNECTED malformado: {}", parts);
            return;
        }
        
        String userId = parts.get(1);
        String username = parts.get(2);
        UserConnectionEvent event = new UserConnectionEvent(userId, username);
        
        logger.info("Usuario conectado: {} ({})", username, userId);
        notifyUserConnectionListeners(event);
    }
    
    private void handleUserDisconnection(List<String> parts) {
        if (parts.size() < 2) {
            logger.warn("Mensaje USER_DISCONNECTED malformado: {}", parts);
            return;
        }
        
        String userId = parts.get(1);
        UserDisconnectionEvent event = new UserDisconnectionEvent(userId);
        
        logger.info("Usuario desconectado: {}", userId);
        notifyUserDisconnectionListeners(event);
    }
    
    private void handlePrivateMessage(List<String> parts) {
        if (parts.size() < 3) {
            logger.warn("Mensaje RECEIVE_PRIVATE_MESSAGE malformado: {}", parts);
            return;
        }
        
        String sender = parts.get(1);
        String content = parts.get(2);
        PrivateMessageEvent event = new PrivateMessageEvent(sender, content);
        
        logger.debug("Mensaje privado recibido de: {}", sender);
        notifyPrivateMessageListeners(event);
    }
    
    private void handlePrivateAudio(List<String> parts) {
        if (parts.size() < 3) {
            logger.warn("Mensaje RECEIVE_PRIVATE_AUDIO malformado: {}", parts);
            return;
        }
        
        String sender = parts.get(1);
        String audioBase64 = parts.get(2);

        try {
            Base64.getDecoder().decode(audioBase64);
        } catch (IllegalArgumentException e) {
            logger.error("Audio base64 inválido recibido de {}: {}", sender, e.getMessage());
            return;
        }
        
        PrivateAudioEvent event = new PrivateAudioEvent(sender, audioBase64);
        
        logger.debug("Audio privado recibido de: {}", sender);
        notifyPrivateAudioListeners(event);
    }

    private void notifyUserConnectionListeners(UserConnectionEvent event) {
        List<UserConnectionListener> listenersCopy;
        synchronized (userConnectionListeners) {
            listenersCopy = new ArrayList<>(userConnectionListeners);
        }
        
        for (UserConnectionListener listener : listenersCopy) {
            try {
                listener.onUserConnected(event);
            } catch (Exception e) {
                logger.error("Error notificando UserConnectionListener: {}", e.getMessage(), e);
            }
        }
    }
    
    private void notifyUserDisconnectionListeners(UserDisconnectionEvent event) {
        List<UserDisconnectionListener> listenersCopy;
        synchronized (userDisconnectionListeners) {
            listenersCopy = new ArrayList<>(userDisconnectionListeners);
        }
        
        for (UserDisconnectionListener listener : listenersCopy) {
            try {
                listener.onUserDisconnected(event);
            } catch (Exception e) {
                logger.error("Error notificando UserDisconnectionListener: {}", e.getMessage(), e);
            }
        }
    }
    
    private void notifyPrivateMessageListeners(PrivateMessageEvent event) {
        List<PrivateMessageListener> listenersCopy;
        synchronized (privateMessageListeners) {
            listenersCopy = new ArrayList<>(privateMessageListeners);
        }
        
        for (PrivateMessageListener listener : listenersCopy) {
            try {
                listener.onPrivateMessageReceived(event);
            } catch (Exception e) {
                logger.error("Error notificando PrivateMessageListener: {}", e.getMessage(), e);
            }
        }
    }
    
    private void notifyPrivateAudioListeners(PrivateAudioEvent event) {
        List<PrivateAudioListener> listenersCopy;
        synchronized (privateAudioListeners) {
            listenersCopy = new ArrayList<>(privateAudioListeners);
        }
        
        for (PrivateAudioListener listener : listenersCopy) {
            try {
                listener.onPrivateAudioReceived(event);
            } catch (Exception e) {
                logger.error("Error notificando PrivateAudioListener: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void registerUserConnectionListener(UserConnectionListener listener) {
        if (listener != null) {
            synchronized (userConnectionListeners) {
                userConnectionListeners.add(listener);
            }
            logger.debug("UserConnectionListener registrado: {}", listener.getClass().getSimpleName());
        }
    }
    
    @Override
    public void registerUserDisconnectionListener(UserDisconnectionListener listener) {
        if (listener != null) {
            synchronized (userDisconnectionListeners) {
                userDisconnectionListeners.add(listener);
            }
            logger.debug("UserDisconnectionListener registrado: {}", listener.getClass().getSimpleName());
        }
    }
    
    @Override
    public void registerPrivateMessageListener(PrivateMessageListener listener) {
        if (listener != null) {
            synchronized (privateMessageListeners) {
                privateMessageListeners.add(listener);
            }
            logger.debug("PrivateMessageListener registrado: {}", listener.getClass().getSimpleName());
        }
    }
    
    @Override
    public void registerPrivateAudioListener(PrivateAudioListener listener) {
        if (listener != null) {
            synchronized (privateAudioListeners) {
                privateAudioListeners.add(listener);
            }
            logger.debug("PrivateAudioListener registrado: {}", listener.getClass().getSimpleName());
        }
    }
}