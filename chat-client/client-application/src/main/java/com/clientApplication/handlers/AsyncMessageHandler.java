package com.clientApplication.handlers;

import com.clientApplication.events.*;
import com.clientApplication.listeners.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AsyncMessageHandler implements MessageHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(AsyncMessageHandler.class);

    private final List<UserConnectionListener> userConnectionListeners = new ArrayList<>();
    private final List<UserDisconnectionListener> userDisconnectionListeners = new ArrayList<>();
    private final List<PrivateMessageListener> privateMessageListeners = new ArrayList<>();
    private final List<PrivateAudioListener> privateAudioListeners = new ArrayList<>();
    private final List<ChannelMessageListener> channelMessageListeners = new ArrayList<>();
    private final List<ChannelAudioListener> channelAudioListeners = new ArrayList<>();
    private final List<InviteListener> inviteListeners = new ArrayList<>();
    
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
                case "RECEIVE_CHANNEL_MESSAGE" -> handleChannelMessage(parts);
                case "RECEIVE_CHANNEL_AUDIO" -> handleChannelAudio(parts);
                case "INVITE_RECEIVED" -> handleInviteReceived(parts);
                case "CHANNEL_MEMBERS_UPDATED" -> handleChannelMembersUpdated(parts);
                default -> logger.warn("Comando asíncrono desconocido: {}", command);
            }
        } catch (Exception e) {
            logger.error("Error procesando mensaje asíncrono [{}]: {}", command, e.getMessage(), e);
        }
    }

    private void handleInviteReceived(List<String> parts) {
        if (parts.size() < 6) { logger.warn("INVITE_RECEIVED malformado: {}", parts); return; }
        int inviteId = Integer.parseInt(parts.get(1));
        int channelId = Integer.parseInt(parts.get(2));
        String channelName = parts.get(3);
        String visibility = parts.get(4);
        String inviterUsername = parts.get(5);
        InviteEvent event = new InviteEvent(inviteId, channelId, channelName, visibility, inviterUsername);
        List<InviteListener> listenersCopy; synchronized (inviteListeners) { listenersCopy = new ArrayList<>(inviteListeners); }
        for (InviteListener l : listenersCopy) { try { l.onInvite(event); } catch (Exception e) { logger.error("Error notificando InviteListener", e); } }
    }

    private void handleChannelMessage(List<String> parts) {
        if (parts.size() < 4) {
            logger.warn("Mensaje RECEIVE_CHANNEL_MESSAGE malformado: {}", parts);
            return;
        }
        int channelId = Integer.parseInt(parts.get(1));
        String sender = parts.get(2);
        String content = parts.get(3);
        ChannelMessageEvent event = new ChannelMessageEvent(channelId, sender, content);
        List<ChannelMessageListener> listenersCopy;
        synchronized (channelMessageListeners) { listenersCopy = new ArrayList<>(channelMessageListeners); }
        for (ChannelMessageListener l : listenersCopy) { try { l.onChannelMessage(event); } catch (Exception e) { logger.error("Error notificando ChannelMessageListener", e); } }
    }

    private void handleChannelAudio(List<String> parts) {
        if (parts.size() < 4) {
            logger.warn("Mensaje RECEIVE_CHANNEL_AUDIO malformado: {}", parts);
            return;
        }
        int channelId = Integer.parseInt(parts.get(1));
        String sender = parts.get(2);
        String audioBase64 = parts.get(3);
        try { Base64.getDecoder().decode(audioBase64); } catch (IllegalArgumentException e) { logger.error("Audio base64 inválido de canal {}: {}", channelId, e.getMessage()); return; }
        ChannelAudioEvent event = new ChannelAudioEvent(channelId, sender, audioBase64);
        List<ChannelAudioListener> listenersCopy;
        synchronized (channelAudioListeners) { listenersCopy = new ArrayList<>(channelAudioListeners); }
        for (ChannelAudioListener l : listenersCopy) { try { l.onChannelAudio(event); } catch (Exception e) { logger.error("Error notificando ChannelAudioListener", e); } }
    }

    private void handleUserConnection(List<String> parts) {
        if (parts.size() < 4) {
            logger.warn("Mensaje USER_CONNECTED malformado: {}", parts);
            return;
        }

        String userId = parts.get(1);
        String username = parts.get(2);
        String photoBase64 = parts.get(3);
        byte[] photoData = null;

        if (photoBase64 != null && !photoBase64.isEmpty()) {
            try {
                photoData = Base64.getDecoder().decode(photoBase64);
            } catch (IllegalArgumentException e) {
                logger.warn("Base64 de foto de perfil inválido para {}: {}", username, e.getMessage());
            }
        }

        UserConnectionEvent event = new UserConnectionEvent(userId, username, photoData);


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

    private void handleChannelMembersUpdated(List<String> parts) {
        if (parts.size() < 2) {
            logger.warn("Mensaje CHANNEL_MEMBERS_UPDATED malformado: {}", parts);
            return;
        }
        int channelId = Integer.parseInt(parts.get(1));
        ChannelMessageEvent event = new ChannelMessageEvent(channelId, "SYSTEM", "MEMBERS_UPDATED");

        List<ChannelMessageListener> listenersCopy;
        synchronized (channelMessageListeners) {
            listenersCopy = new ArrayList<>(channelMessageListeners);
        }
        for (ChannelMessageListener l : listenersCopy) {
            try {
                l.onChannelMessage(event);
            } catch (Exception e) {
                logger.error("Error notificando ChannelMessageListener para actualización de miembros", e);
            }
        }
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

    public void registerChannelMessageListener(ChannelMessageListener listener) {
        if (listener != null) { synchronized (channelMessageListeners) { channelMessageListeners.add(listener); } }
    }
    public void registerChannelAudioListener(ChannelAudioListener listener) {
        if (listener != null) { synchronized (channelAudioListeners) { channelAudioListeners.add(listener); } }
    }
    public void registerInviteListener(InviteListener listener) { if (listener != null) { synchronized (inviteListeners) { inviteListeners.add(listener); } } }
    
    // Métodos para limpiar listeners al desconectar
    public void clearAllListeners() {
        synchronized (userConnectionListeners) {
            userConnectionListeners.clear();
        }
        synchronized (userDisconnectionListeners) {
            userDisconnectionListeners.clear();
        }
        synchronized (privateMessageListeners) {
            privateMessageListeners.clear();
        }
        synchronized (privateAudioListeners) {
            privateAudioListeners.clear();
        }
        synchronized (channelMessageListeners) {
            channelMessageListeners.clear();
        }
        synchronized (channelAudioListeners) {
            channelAudioListeners.clear();
        }
        synchronized (inviteListeners) {
            inviteListeners.clear();
        }
        logger.debug("Todos los listeners han sido limpiados");
    }
}