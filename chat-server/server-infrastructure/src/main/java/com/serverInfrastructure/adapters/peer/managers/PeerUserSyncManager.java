package com.serverInfrastructure.adapters.peer.managers;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.dto.UserSyncInfo;
import com.serverApplication.dto.sync.*;
import com.serverInfrastructure.adapters.peer.user.LocalUserRepository;
import com.serverInfrastructure.adapters.peer.user.RemoteUserPhotoStore;
import com.serverInfrastructure.adapters.peer.user.ServerPrefixFormatter;
import com.serverInfrastructure.persistence.dao.ChannelDAO;
import com.serverInfrastructure.persistence.dao.ChannelInviteDAO;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.persistence.dao.UserDAO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PeerUserSyncManager {

    private static final Logger logger = LoggerFactory.getLogger(PeerUserSyncManager.class);
    private final ProtocolParser protocolParser;
    private final Map<String, List<String>> remoteServerUsers = new ConcurrentHashMap<>();
    private final RemoteUserPhotoStore photoStore;
    private final LocalUserRepository userRepository;

    private final UserDAO userDAO;
    private final ChannelDAO channelDAO;
    private final ChannelInviteDAO channelInviteDAO;
    private final MessageDAO messageDAO;
    private final ObjectMapper objectMapper;

    private String localServerId;
    private Consumer<String> peerBroadcastCallback;
    private Consumer<String> clientBroadcastCallback;
    private Consumer<Void> uiUpdateCallback;

    public PeerUserSyncManager() {
        this.protocolParser = new ProtocolParser('|', '\\');
        this.photoStore = new RemoteUserPhotoStore();
        this.userRepository = new LocalUserRepository();

        this.userDAO = new UserDAO();
        this.channelDAO = new ChannelDAO();
        this.channelInviteDAO = new ChannelInviteDAO();
        this.messageDAO = new MessageDAO();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    public void setLocalServerId(String serverId) {
        this.localServerId = serverId;
        logger.info("ID del servidor local establecido: {}", serverId);
    }

    public void setPeerBroadcastCallback(Consumer<String> callback) {
        this.peerBroadcastCallback = callback;
    }

    public void setClientBroadcastCallback(Consumer<String> callback) {
        this.clientBroadcastCallback = callback;
    }

    public void setUiUpdateCallback(Consumer<Void> callback) {
        this.uiUpdateCallback = callback;
    }

    public void handleUserSyncMessage(String peerId, String message) {
        if (message.startsWith("P2P_DB_SYNC|")) {
            handleFullDatabaseSync(peerId, message);
            return;
        }

        try {
            UserSyncInfo syncInfo = UserSyncInfo.fromProtocol(message);

            String action = syncInfo.action();
            List<String> users = syncInfo.connectedUsers();
            Map<String, String> photos = syncInfo.userPhotos();
            String serverPrefix = ServerPrefixFormatter.createPrefix(peerId);

            if (photos != null && !photos.isEmpty()) {
                photoStore.storePhotos(photos);
            }

            if ("SYNC_ALL".equals(action)) {
                handleFullSync(peerId, users, serverPrefix);
            } else if ("USER_JOINED".equals(action)) {
                handleUserJoined(peerId, users, serverPrefix);
            } else if ("USER_LEFT".equals(action)) {
                handleUserLeft(peerId, users, serverPrefix);
            }

            logRemoteUsersState();

        } catch (Exception e) {
            logger.error("Error procesando mensaje de sincronización de usuarios: {}", e.getMessage(), e);
        }
    }

    private void handleFullDatabaseSync(String peerId, String message) {
        try {
            String json = message.substring("P2P_DB_SYNC|".length());
            FullDatabaseSyncDTO syncData = objectMapper.readValue(json, FullDatabaseSyncDTO.class);

            logger.info("Recibida sincronización completa de BD de peer {}. Procesando...", peerId);

            // Sync Users
            if (syncData.users() != null) {
                for (UserSyncDTO userDTO : syncData.users()) {
                    try {
                        if (userDAO.findByUsername(new com.serverDomain.valueObjects.Username(userDTO.username()))
                                .isEmpty()) {
                            com.serverDomain.entities.User user = new com.serverDomain.entities.User(
                                    userDTO.userId(),
                                    new com.serverDomain.valueObjects.Username(userDTO.username()),
                                    new com.serverDomain.valueObjects.Email(userDTO.email()),
                                    userDTO.passwordHash(),
                                    userDTO.photoData(),
                                    userDTO.ipAddress(),
                                    userDTO.createdAt());
                            user.setReplicated(true);
                            user.setOriginServerId(
                                    userDTO.originServerId() != null ? userDTO.originServerId() : peerId);
                            user.setLastSyncAt(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
                            userDAO.insertReplicated(user);
                        }
                    } catch (Exception e) {
                        logger.error("Error syncing user {}: {}", userDTO.username(), e.getMessage());
                    }
                }
            }

            // Sync Channels
            if (syncData.channels() != null) {
                for (ChannelSyncDTO channelDTO : syncData.channels()) {
                    try {
                        if (channelDAO.findById(channelDTO.channelId()).isEmpty()) {
                            com.serverDomain.entities.Channel channel = new com.serverDomain.entities.Channel(
                                    channelDTO.channelId(),
                                    channelDTO.name(),
                                    channelDTO.ownerId(),
                                    com.serverDomain.entities.Channel.Visibility.valueOf(channelDTO.visibility()),
                                    channelDTO.createdAt());
                            channelDAO.insertReplicated(channel);
                        }
                    } catch (Exception e) {
                        logger.error("Error syncing channel {}: {}", channelDTO.channelId(), e.getMessage());
                    }
                }
            }

            // Sync Channel Members
            if (syncData.channelMembers() != null) {
                for (ChannelMemberSyncDTO memberDTO : syncData.channelMembers()) {
                    try {
                        if (!channelDAO.isMember(memberDTO.channelId(), memberDTO.userId())) {
                            channelDAO.addMember(memberDTO.channelId(), memberDTO.userId());
                        }
                    } catch (Exception e) {
                        logger.error("Error syncing channel member: {}", e.getMessage());
                    }
                }
            }

            // Sync Channel Invites
            if (syncData.channelInvites() != null) {
                for (ChannelInviteSyncDTO inviteDTO : syncData.channelInvites()) {
                    try {
                        com.serverDomain.entities.ChannelInvite invite = new com.serverDomain.entities.ChannelInvite(
                                inviteDTO.inviteId(),
                                inviteDTO.channelId(),
                                inviteDTO.inviterUserId(),
                                inviteDTO.invitedUserId(),
                                com.serverDomain.entities.ChannelInvite.Status.valueOf(inviteDTO.status()),
                                inviteDTO.createdAt());
                        channelInviteDAO.insertReplicated(invite);
                    } catch (Exception e) {
                        // Likely duplicate, ignore
                    }
                }
            }

            // Messages
            if (syncData.messages() != null) {
                for (MessageSyncDTO messageDTO : syncData.messages()) {
                    try {
                        messageDAO.insertReplicated(messageDTO);
                    } catch (Exception e) {
                    }
                }
            }

            // Transcriptions
            if (syncData.transcriptions() != null) {
                for (AudioTranscriptionSyncDTO transDTO : syncData.transcriptions()) {
                    try {
                        messageDAO.insertTranscriptionReplicated(transDTO);
                    } catch (Exception e) {
                    }
                }
            }

            logger.info("Sincronización de BD completada con peer {}", peerId);

            if (uiUpdateCallback != null) {
                uiUpdateCallback.accept(null);
                logger.info("UI notificada de actualización de usuarios tras sincronización de BD");
            }

        } catch (Exception e) {
            logger.error("Error procesando sincronización completa de BD: {}", e.getMessage(), e);
        }
    }

    private void handleFullSync(String peerId, List<String> users, String serverPrefix) {
        List<String> previousUsers = remoteServerUsers.put(peerId, new ArrayList<>(users));
        logger.info("Sincronización completa recibida de {}: {} usuario(s)", peerId, users.size());

        notifyClientsAboutRemoteUsers(peerId, users, serverPrefix, "SYNC_ALL", previousUsers);
    }

    private void handleUserJoined(String peerId, List<String> users, String serverPrefix) {
        remoteServerUsers.computeIfAbsent(peerId, k -> new ArrayList<>()).addAll(users);
        logger.info("Usuario(s) {} se unió a peer {}", users, peerId);

        notifyClientsAboutRemoteUsers(peerId, users, serverPrefix, "USER_JOINED", null);
    }

    private void handleUserLeft(String peerId, List<String> users, String serverPrefix) {
        List<String> peerUsers = remoteServerUsers.get(peerId);
        if (peerUsers != null) {
            peerUsers.removeAll(users);
            logger.info("Usuario(s) {} salió de peer {}", users, peerId);

            notifyClientsAboutRemoteUsers(peerId, users, serverPrefix, "USER_LEFT", null);
        }
    }

    private void notifyClientsAboutRemoteUsers(String peerId, List<String> users, String serverPrefix, String action,
            List<String> previousUsers) {
        try {
            if ("SYNC_ALL".equals(action)) {
                logger.info("Sincronización completa: {} usuarios remotos de {}", users.size(), serverPrefix);
            } else if ("USER_JOINED".equals(action)) {
                logger.info("Usuario remoto conectado: {} {}", serverPrefix, String.join(", ", users));
            } else if ("USER_LEFT".equals(action)) {
                logger.info("Usuario remoto desconectado: {} {}", serverPrefix, String.join(", ", users));
            }

            if (clientBroadcastCallback != null) {
                for (String username : users) {
                    String uniqueId = peerId + "-" + username;
                    if ("USER_LEFT".equals(action)) {
                        String msg = protocolParser.encode("USER_DISCONNECTED", uniqueId, serverPrefix + username);
                        clientBroadcastCallback.accept(msg);
                        photoStore.removePhoto(username);
                    } else {
                        String photoBase64 = photoStore.getPhoto(username);
                        String msg = protocolParser.encode("USER_CONNECTED", uniqueId, serverPrefix + username,
                                photoBase64);
                        clientBroadcastCallback.accept(msg);
                    }
                }
            } else {
                logger.warn("clientBroadcastCallback es NULL - no se puede notificar a clientes locales");
            }
        } catch (Exception e) {
            logger.error("Error notificando usuarios remotos: {}", e.getMessage(), e);
        }
    }

    public void notifyUserChangeToPeers(String username, String action, boolean hasPeersConnected) {
        if (!hasPeersConnected) {
            logger.debug("No hay peers conectados - no se requiere notificación");
            return;
        }

        String serverId = getServerIdOrDefault();

        UserSyncInfo syncInfo;
        if ("USER_JOINED".equals(action)) {
            String photoBase64 = userRepository.getUserPhoto(username);
            syncInfo = UserSyncInfo.createUserJoined(serverId, username, photoBase64);
        } else if ("USER_LEFT".equals(action)) {
            syncInfo = UserSyncInfo.createUserLeft(serverId, username);
        } else {
            logger.warn("Acción desconocida para sincronización de usuarios: {}", action);
            return;
        }

        String message = syncInfo.toProtocol();

        if (peerBroadcastCallback != null) {
            peerBroadcastCallback.accept(message);
            logger.info("Notificación de {} {} enviada a peers", action, username);
        }
    }

    public void sendFullUserSyncToPeer(String peerId, Consumer<String> sendCallback) {
        // This method was for user sync only. We might want to keep it or upgrade it.
        // But for now, let's keep it as is or redirect to full DB sync if needed.
        // However, this is called by PeerUserReplicationManager which handles batch
        // user replication.
        // The requirement is about "discovering a new server".
        // So generateInitialSyncMessage is the key.

        Map<String, String> localUsersWithPhotos = userRepository.getLocalUsersWithPhotos();
        List<String> localUsers = new ArrayList<>(localUsersWithPhotos.keySet());
        String serverId = getServerIdOrDefault();

        UserSyncInfo syncInfo = UserSyncInfo.createFullSync(serverId, localUsers, localUsersWithPhotos);
        String message = syncInfo.toProtocol();

        sendCallback.accept(message);
        logger.info("Sincronización completa de {} usuario(s) enviada a peer {}", localUsers.size(), peerId);
    }

    public String generateInitialSyncMessage(int peerPort) {
        try {
            String serverId = localServerId;
            if (serverId == null) {
                try {
                    serverId = java.net.InetAddress.getLocalHost().getHostAddress() + ":" + peerPort;
                } catch (Exception e) {
                    serverId = "localhost:" + peerPort;
                }
            }

            // Collect all data
            List<UserSyncDTO> users = userDAO.selectAll().stream().map(u -> new UserSyncDTO(
                    u.getId(), u.getUsername().value(), u.getEmail().value(), u.getPasswordHash(),
                    u.getPhotoData(), u.getIpAddress(), u.isReplicated(), u.getOriginServerId(),
                    u.getLastSyncAt() != null ? u.getLastSyncAt().toLocalDateTime() : null,
                    u.getCreatedAt())).collect(Collectors.toList());

            List<ChannelSyncDTO> channels = channelDAO.findAll().stream().map(c -> new ChannelSyncDTO(
                    c.getId(), c.getName(), c.getOwnerId(), c.getVisibility().name(), c.getCreatedAt()))
                    .collect(Collectors.toList());

            List<ChannelMemberSyncDTO> members = channelDAO.findAllMembers().stream()
                    .map(m -> new ChannelMemberSyncDTO(m.channelId(), m.userId()))
                    .collect(Collectors.toList());

            List<ChannelInviteSyncDTO> invites = channelInviteDAO.findAll().stream()
                    .map(i -> new ChannelInviteSyncDTO(
                            i.getId(), i.getChannelId(), i.getInviterUserId(), i.getInvitedUserId(),
                            i.getStatus().name(), i.getCreatedAt()))
                    .collect(Collectors.toList());

            List<MessageSyncDTO> messages = messageDAO.findAll();

            List<AudioTranscriptionSyncDTO> transcriptions = messageDAO.findAllTranscriptions();

            FullDatabaseSyncDTO syncData = new FullDatabaseSyncDTO(users, channels, members, invites, messages,
                    transcriptions);

            String json = objectMapper.writeValueAsString(syncData);
            return "P2P_DB_SYNC|" + json;

        } catch (Exception e) {
            logger.error("Error generando mensaje de sincronización inicial: {}", e.getMessage());
            return "";
        }
    }

    public Map<String, List<String>> getAllUsersAcrossPeers() {
        return new HashMap<>(remoteServerUsers);
    }

    public String getRemoteUserPhoto(String username) {
        return photoStore.getPhoto(username);
    }

    public void handlePeerDisconnection(String peerId) {
        List<String> disconnectedUsers = remoteServerUsers.get(peerId);

        if (disconnectedUsers == null || disconnectedUsers.isEmpty()) {
            logger.info("Peer {} se desconectó, pero no tenía usuarios remotos registrados o ya fueron limpiados.",
                    peerId);
            clearPeerUsers(peerId);
            return;
        }

        List<String> usersToNotify = new ArrayList<>(disconnectedUsers);
        clearPeerUsers(peerId);

        logger.info("Peer {} se desconectó. Se notificará la desconexión de {} usuario(s) remoto(s).", peerId,
                usersToNotify.size());

        if (clientBroadcastCallback == null) {
            logger.warn(
                    "No se puede notificar a los clientes sobre la desconexión de usuarios remotos: el callback es nulo.");
            return;
        }

        String serverPrefix = ServerPrefixFormatter.createPrefix(peerId);
        for (String username : usersToNotify) {
            String uniqueId = peerId + "-" + username;
            String fullUsername = serverPrefix + username;
            String message = protocolParser.encode("USER_DISCONNECTED", uniqueId, fullUsername);

            clientBroadcastCallback.accept(message);
        }

        logger.info("Notificaciones de desconexión para los usuarios de {} enviadas a los clientes locales.", peerId);
    }

    public void clearPeerUsers(String peerId) {
        remoteServerUsers.remove(peerId);
        logger.info("Usuarios de peer {} eliminados del registro de sincronización.", peerId);
    }

    private String getServerIdOrDefault() {
        if (localServerId != null) {
            return localServerId;
        }

        try {
            return java.net.InetAddress.getLocalHost().getHostAddress() + ":0";
        } catch (Exception e) {
            return "localhost:0";
        }
    }

    private void logRemoteUsersState() {
        try {
            StringBuilder sb = new StringBuilder();
            remoteServerUsers.forEach((k, v) -> sb.append(k).append("=").append(v.size()).append(" "));
            logger.info("Estado usuarios remotos: {}", sb.toString().trim());
        } catch (Exception ignore) {
        }
    }
}
