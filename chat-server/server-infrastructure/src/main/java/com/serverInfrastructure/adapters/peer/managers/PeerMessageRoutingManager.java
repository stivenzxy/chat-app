package com.serverInfrastructure.adapters.peer.managers;

import com.chatCommon.protocol.ProtocolParser;
import com.serverDomain.entities.User;
import com.serverApplication.ports.peer.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class PeerMessageRoutingManager {
    private static final Logger logger = LoggerFactory.getLogger(PeerMessageRoutingManager.class);

    private final ProtocolParser protocolParser;

    private ClientMessageBroadcaster clientBroadcaster;
    private RemoteUsersProvider remoteUsersProvider;
    private PeerMessageSender peerMessageSender;
    
    public PeerMessageRoutingManager() {
        this.protocolParser = new ProtocolParser('|', '\\');
    }

    public void setClientBroadcaster(ClientMessageBroadcaster broadcaster) {
        this.clientBroadcaster = broadcaster;
        logger.info("ClientMessageBroadcaster configurado en PeerMessageRoutingManager");
    }

    public void setRemoteUsersProvider(RemoteUsersProvider provider) {
        this.remoteUsersProvider = provider;
        logger.info("RemoteUsersProvider configurado en PeerMessageRoutingManager");
    }

    public void setPeerMessageSender(PeerMessageSender sender) {
        this.peerMessageSender = sender;
        logger.info("PeerMessageSender configurado en PeerMessageRoutingManager");
    }

    public boolean routePrivateMessageToPeer(String recipientUsername, String routeMessage) {
        return routeToPeer(recipientUsername, routeMessage, "mensaje");
    }

    public boolean routePrivateAudioToPeer(String recipientUsername, String routeMessage) {
        return routeToPeer(recipientUsername, routeMessage, "audio");
    }

    private boolean routeToPeer(String recipientUsername, String routeMessage, String messageType) {
        if (remoteUsersProvider == null || peerMessageSender == null) {
            logger.error("Router no configurado correctamente (faltan dependencias)");
            return false;
        }

        Map<String, List<String>> remoteServerUsers = remoteUsersProvider.getRemoteUsers();
        for (Map.Entry<String, List<String>> entry : remoteServerUsers.entrySet()) {
            String peerId = entry.getKey();
            List<String> users = entry.getValue();
            
            if (users.contains(recipientUsername)) {
                logger.info("Enrutando {} privado a usuario {} en peer {}", messageType, recipientUsername, peerId);

                peerMessageSender.sendToPeer(peerId, routeMessage);
                logger.info("Mensaje de tipo: {} enrutado exitosamente a peer {}", messageType, peerId);
                return true;
            }
        }
        
        logger.warn("Usuario {} no encontrado en ningún peer remoto", recipientUsername);
        return false;
    }
    

    public void handlePrivateMessageRouted(String sourcePeerId, String message) {
        try {
            List<String> parts = protocolParser.decode(message);
            
            if (parts.size() < 4) {
                logger.error("Formato inválido de mensaje P2P_ROUTE_PRIVATE: {}", message);
                return;
            }
            
            String senderUsername = parts.get(1);
            String recipientUsername = parts.get(2);
            String content = parts.get(3);
            
            logger.info("Entregando mensaje de {} a usuario local {}", senderUsername, recipientUsername);
            
            if (clientBroadcaster == null) {
                logger.warn("No hay broadcaster configurado para entregar mensaje");
                return;
            }

            String serverPrefix = "Servidor " + sourcePeerId.split(":")[0] + " - ";
            String senderWithPrefix = serverPrefix + senderUsername;

            String deliverMessage = protocolParser.encode("RECEIVE_PRIVATE_MESSAGE", senderWithPrefix, content);

            deliverLocalMessage(recipientUsername, deliverMessage);
            
        } catch (Exception e) {
            logger.error("Error procesando mensaje privado enrutado: {}", e.getMessage());
        }
    }

    public void handlePrivateAudioRouted(String sourcePeerId, String message) {
        try {
            List<String> parts = protocolParser.decode(message);
            
            if (parts.size() < 4) {
                logger.error("Formato inválido de mensaje P2P_ROUTE_PRIVATE_AUDIO: {}", message);
                return;
            }
            
            String senderUsername = parts.get(1);
            String recipientUsername = parts.get(2);
            String audioBase64 = parts.get(3);
            
            logger.info("Entregando audio de {} a usuario local {}", senderUsername, recipientUsername);
            
            if (clientBroadcaster == null) {
                logger.warn("No hay callback de broadcast configurado para entregar audio");
                return;
            }

            String serverPrefix = "Servidor " + sourcePeerId.split(":")[0] + " - ";
            String senderWithPrefix = serverPrefix + senderUsername;
            String deliverMessage = protocolParser.encode("RECEIVE_PRIVATE_AUDIO", senderWithPrefix, audioBase64);

            deliverLocalMessage(recipientUsername, deliverMessage);
        } catch (Exception e) {
            logger.error("Error procesando audio privado enrutado: {}", e.getMessage());
        }
    }

    private void deliverLocalMessage(String recipientUsername, String deliverMessage) {
        try {
            com.serverInfrastructure.observers.ActiveUserManager aum = 
                com.serverInfrastructure.observers.ActiveUserManager.getInstance();
            
            List<User> userSessions = aum.getUserSessions(recipientUsername);
            
            if (userSessions.isEmpty()) {
                logger.warn("Usuario destinatario {} no está conectado localmente", recipientUsername);
                return;
            }

            for (User user : userSessions) {
                String connectionId = user.getId();
                logger.info("Mensaje/audio enrutado para sesión {} de {}", connectionId, recipientUsername);
            }

            clientBroadcaster.broadcast(null, deliverMessage, null);
            logger.info("Mensaje/audio enrutado transmitido para entrega local a {}", recipientUsername);
            
        } catch (Exception e) {
            logger.error("Error entregando mensaje enrutado: {}", e.getMessage());
        }
    }

    /**
     * Enruta una invitación de canal a un usuario remoto.
     * Formato: P2P_CHANNEL_INVITE|inviteId|channelId|channelName|visibility|inviterUsername|invitedUsername
     */
    public boolean routeChannelInviteToPeer(String recipientUsername, String routeMessage) {
        if (remoteUsersProvider == null || peerMessageSender == null) {
            logger.error("Router no configurado correctamente (faltan dependencias)");
            return false;
        }

        Map<String, List<String>> remoteServerUsers = remoteUsersProvider.getRemoteUsers();
        for (Map.Entry<String, List<String>> entry : remoteServerUsers.entrySet()) {
            String peerId = entry.getKey();
            List<String> users = entry.getValue();
            
            if (users.contains(recipientUsername)) {
                logger.info("Enrutando invitación de canal a usuario {} en peer {}", recipientUsername, peerId);
                peerMessageSender.sendToPeer(peerId, routeMessage);
                logger.info("Invitación de canal enrutada exitosamente a peer {}", peerId);
                return true;
            }
        }
        
        logger.warn("Usuario {} no encontrado en ningún peer remoto", recipientUsername);
        return false;
    }

    /**
     * Maneja una invitación de canal recibida desde un peer remoto.
     * Formato: P2P_CHANNEL_INVITE|inviteId|channelId|channelName|visibility|inviterUsername|invitedUsername
     */
    public void handleChannelInviteRouted(String sourcePeerId, String message) {
        try {
            List<String> parts = protocolParser.decode(message);
            
            if (parts.size() < 7) {
                logger.error("Formato inválido de mensaje P2P_CHANNEL_INVITE: {}", message);
                return;
            }
            
            String inviteId = parts.get(1);
            String channelId = parts.get(2);
            String channelName = parts.get(3);
            String visibility = parts.get(4);
            String inviterUsername = parts.get(5);
            String invitedUsername = parts.get(6);
            
            logger.info("Entregando invitación de canal {} de {} a usuario local {}", channelName, inviterUsername, invitedUsername);
            
            if (clientBroadcaster == null) {
                logger.warn("No hay broadcaster configurado para entregar invitación");
                return;
            }

            String serverPrefix = "Servidor " + sourcePeerId.split(":")[0] + " - ";
            String inviterWithPrefix = serverPrefix + inviterUsername;
            
            // Formato: INVITE_RECEIVED|inviteId|channelId|channelName|visibility|inviterUsername
            String deliverMessage = protocolParser.encode("INVITE_RECEIVED", 
                    inviteId, channelId, channelName, visibility, inviterWithPrefix);

            deliverLocalMessage(invitedUsername, deliverMessage);
            
        } catch (Exception e) {
            logger.error("Error procesando invitación de canal enrutada: {}", e.getMessage());
        }
    }

    /**
     * Enruta un mensaje de canal a un usuario remoto.
     * Formato: P2P_CHANNEL_MESSAGE|channelId|senderUsername|content|recipientUsername
     */
    public boolean routeChannelMessageToPeer(String recipientUsername, String routeMessage) {
        if (remoteUsersProvider == null || peerMessageSender == null) {
            logger.error("Router no configurado correctamente (faltan dependencias)");
            return false;
        }

        Map<String, List<String>> remoteServerUsers = remoteUsersProvider.getRemoteUsers();
        for (Map.Entry<String, List<String>> entry : remoteServerUsers.entrySet()) {
            String peerId = entry.getKey();
            List<String> users = entry.getValue();
            
            if (users.contains(recipientUsername)) {
                logger.info("Enrutando mensaje de canal a usuario {} en peer {}", recipientUsername, peerId);
                peerMessageSender.sendToPeer(peerId, routeMessage);
                return true;
            }
        }
        
        logger.warn("Usuario {} no encontrado en ningún peer remoto para mensaje de canal", recipientUsername);
        return false;
    }

    /**
     * Maneja un mensaje de canal recibido desde un peer remoto.
     * Formato: P2P_CHANNEL_MESSAGE|channelId|senderUsername|content|recipientUsername
     */
    public void handleChannelMessageRouted(String sourcePeerId, String message) {
        try {
            List<String> parts = protocolParser.decode(message);
            
            if (parts.size() < 5) {
                logger.error("Formato inválido de mensaje P2P_CHANNEL_MESSAGE: {}", message);
                return;
            }
            
            String channelId = parts.get(1);
            String senderUsername = parts.get(2);
            String content = parts.get(3);
            String recipientUsername = parts.get(4);
            
            logger.info("Entregando mensaje de canal de {} a usuario local {}", senderUsername, recipientUsername);
            
            if (clientBroadcaster == null) {
                logger.warn("No hay broadcaster configurado para entregar mensaje de canal");
                return;
            }

            // Verificar si es una notificación especial del sistema
            if ("SYSTEM".equals(senderUsername) && content.startsWith("MEMBER_JOINED:")) {
                // Extraer el nombre del nuevo miembro
                String newMemberUsername = content.substring("MEMBER_JOINED:".length());
                // Formato: CHANNEL_MEMBERS_UPDATED|channelId|newMemberUsername
                String deliverMessage = protocolParser.encode("CHANNEL_MEMBERS_UPDATED", 
                        channelId, newMemberUsername);
                deliverLocalMessage(recipientUsername, deliverMessage);
            } else {
                // Mensaje normal de canal
                String serverPrefix = "Servidor " + sourcePeerId.split(":")[0] + " - ";
                String senderWithPrefix = serverPrefix + senderUsername;
                
                // Formato: RECEIVE_CHANNEL_MESSAGE|channelId|senderUsername|content
                String deliverMessage = protocolParser.encode("RECEIVE_CHANNEL_MESSAGE", 
                        channelId, senderWithPrefix, content);

                deliverLocalMessage(recipientUsername, deliverMessage);
            }
            
        } catch (Exception e) {
            logger.error("Error procesando mensaje de canal enrutado: {}", e.getMessage());
        }
    }
}