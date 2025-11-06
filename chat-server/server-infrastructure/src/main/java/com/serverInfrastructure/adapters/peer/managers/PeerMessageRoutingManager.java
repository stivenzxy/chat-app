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
}