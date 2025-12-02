package com.serverInfrastructure.adapters.peer.managers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverApplication.dto.sync.AudioTranscriptionSyncDTO;
import com.serverApplication.dto.sync.ChannelInviteSyncDTO;
import com.serverApplication.dto.sync.ChannelMemberSyncDTO;
import com.serverApplication.dto.sync.ChannelSyncDTO;
import com.serverApplication.dto.sync.MessageSyncDTO;
import com.serverDomain.entities.Channel;
import com.serverDomain.entities.ChannelInvite;
import com.serverInfrastructure.persistence.dao.ChannelDAO;
import com.serverInfrastructure.persistence.dao.ChannelInviteDAO;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

public class PeerEntityReplicationManager {
    private static final Logger logger = LoggerFactory.getLogger(PeerEntityReplicationManager.class);

    private final ChannelDAO channelDAO;
    private final MessageDAO messageDAO;
    private final ChannelInviteDAO channelInviteDAO;
    private final ObjectMapper objectMapper;

    private BiConsumer<String, String> peerMessageSender;
    private Runnable broadcastCallback;

    public PeerEntityReplicationManager() {
        this.channelDAO = new ChannelDAO();
        this.messageDAO = new MessageDAO();
        this.channelInviteDAO = new ChannelInviteDAO();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void setPeerMessageSender(BiConsumer<String, String> sender) {
        this.peerMessageSender = sender;
    }

    public void setBroadcastCallback(Runnable callback) {
        this.broadcastCallback = callback;
    }

    public void setBroadcaster(java.util.function.Consumer<String> broadcaster) {
        this.broadcaster = broadcaster;
        logger.info("Broadcaster configurado en PeerEntityReplicationManager");
    }

    private java.util.function.Consumer<String> broadcaster;

    public void broadcastChannel(Channel channel) {
        try {
            ChannelSyncDTO dto = new ChannelSyncDTO(
                    channel.getId(),
                    channel.getName(),
                    channel.getOwnerId(),
                    channel.getVisibility().name(),
                    channel.getCreatedAt());
            String json = objectMapper.writeValueAsString(dto);
            String message = "P2P_REPLICATE_CHANNEL|" + json;
            logger.info("Intentando difundir canal: {}", message);
            if (broadcaster != null) {
                broadcaster.accept(message);
                logger.info("Canal enviado al broadcaster");
            } else {
                logger.warn("Broadcaster es nulo en PeerEntityReplicationManager");
            }
        } catch (Exception e) {
            logger.error("Error broadcasting channel: {}", e.getMessage());
        }
    }

    public void broadcastChannelMember(String channelId, String userId) {
        try {
            ChannelMemberSyncDTO dto = new ChannelMemberSyncDTO(channelId, userId);
            String json = objectMapper.writeValueAsString(dto);
            String message = "P2P_REPLICATE_CHANNEL_MEMBER|" + json;
            if (broadcaster != null)
                broadcaster.accept(message);
        } catch (Exception e) {
            logger.error("Error broadcasting channel member: {}", e.getMessage());
        }
    }

    public void broadcastChannelMessage(MessageSyncDTO messageDTO) {
        try {
            String json = objectMapper.writeValueAsString(messageDTO);
            String message = "P2P_REPLICATE_MESSAGE|" + json;
            if (broadcaster != null)
                broadcaster.accept(message);
        } catch (Exception e) {
            logger.error("Error broadcasting message: {}", e.getMessage());
        }
    }

    public void broadcastChannelInvite(ChannelInvite invite) {
        try {
            ChannelInviteSyncDTO dto = new ChannelInviteSyncDTO(
                    invite.getId(),
                    invite.getChannelId(),
                    invite.getInviterUserId(),
                    invite.getInvitedUserId(),
                    invite.getStatus().name(),
                    invite.getCreatedAt());
            String json = objectMapper.writeValueAsString(dto);
            String message = "P2P_REPLICATE_INVITE|" + json;
            if (broadcaster != null)
                broadcaster.accept(message);
        } catch (Exception e) {
            logger.error("Error broadcasting invite: {}", e.getMessage());
        }
    }

    public void broadcastTranscription(AudioTranscriptionSyncDTO dto) {
        try {
            String json = objectMapper.writeValueAsString(dto);
            String message = "P2P_REPLICATE_TRANSCRIPTION|" + json;
            if (broadcaster != null)
                broadcaster.accept(message);
        } catch (Exception e) {
            logger.error("Error broadcasting transcription: {}", e.getMessage());
        }
    }

    public void handleIncomingReplication(String peerId, String message) {
        try {
            if (message.startsWith("P2P_REPLICATE_CHANNEL|")) {
                String json = message.substring("P2P_REPLICATE_CHANNEL|".length());
                ChannelSyncDTO dto = objectMapper.readValue(json, ChannelSyncDTO.class);
                Channel channel = new Channel(dto.channelId(), dto.name(), dto.ownerId(),
                        Channel.Visibility.valueOf(dto.visibility()), dto.createdAt());

                if (channelDAO.findById(channel.getId()).isEmpty()) {
                    try {
                        channelDAO.insertReplicated(channel);
                        logger.info("Canal replicado recibido de {}: {}", peerId, channel.getName());
                    } catch (Exception e) {
                        logger.error("Error insertando canal replicado (posible falta de usuario owner {}): {}",
                                channel.getOwnerId(), e.getMessage());
                    }
                } else {
                    logger.debug("Canal {} ya existe, ignorando replicación", channel.getId());
                }

            } else if (message.startsWith("P2P_REPLICATE_CHANNEL_MEMBER|")) {
                String json = message.substring("P2P_REPLICATE_CHANNEL_MEMBER|".length());
                ChannelMemberSyncDTO dto = objectMapper.readValue(json, ChannelMemberSyncDTO.class);
                try {
                    channelDAO.addMember(dto.channelId(), dto.userId());
                    logger.info("Miembro de canal replicado recibido de {}: {} -> {}", peerId, dto.userId(),
                            dto.channelId());
                } catch (Exception e) {
                    logger.error("Error insertando miembro de canal replicado: {}", e.getMessage());
                }

            } else if (message.startsWith("P2P_REPLICATE_MESSAGE|")) {
                String json = message.substring("P2P_REPLICATE_MESSAGE|".length());
                MessageSyncDTO dto = objectMapper.readValue(json, MessageSyncDTO.class);
                messageDAO.insertReplicated(dto);
                logger.info("Mensaje replicado recibido de {}", peerId);

            } else if (message.startsWith("P2P_REPLICATE_INVITE|")) {
                String json = message.substring("P2P_REPLICATE_INVITE|".length());
                ChannelInviteSyncDTO dto = objectMapper.readValue(json, ChannelInviteSyncDTO.class);
                com.serverDomain.entities.ChannelInvite invite = new com.serverDomain.entities.ChannelInvite(
                        dto.inviteId(),
                        dto.channelId(),
                        dto.inviterUserId(),
                        dto.invitedUserId(),
                        com.serverDomain.entities.ChannelInvite.Status.valueOf(dto.status()),
                        dto.createdAt());
                channelInviteDAO.insertReplicated(invite);
                logger.info("Invitación replicada recibida de {}", peerId);
            } else if (message.startsWith("P2P_REPLICATE_TRANSCRIPTION|")) {
                String json = message.substring("P2P_REPLICATE_TRANSCRIPTION|".length());
                AudioTranscriptionSyncDTO dto = objectMapper.readValue(json, AudioTranscriptionSyncDTO.class);
                messageDAO.insertTranscriptionReplicated(dto);
                logger.info("Transcripción replicada recibida de {}", peerId);
            }
        } catch (Exception e) {
            logger.error("Error handling incoming replication from {}: {}", peerId, e.getMessage());
        }
    }
}
