package com.serverInfrastructure.adapters.peer.managers;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.dto.UserSyncInfo;
import com.serverDomain.entities.User;
import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PeerUserSyncManager {

    private static final Logger logger = LoggerFactory.getLogger(PeerUserSyncManager.class);
    private final ProtocolParser protocolParser;
    private final Map<String, List<String>> remoteServerUsers = new ConcurrentHashMap<>();
    private final Map<String, String> lastReceivedPhotos = new ConcurrentHashMap<>();

    private String localServerId;
    public PeerUserSyncManager() {
        this.protocolParser = new ProtocolParser('|', '\\');
    }
    private Consumer<String> peerBroadcastCallback;

    private Consumer<String> clientBroadcastCallback;

    private Supplier<List<String>> localUsersProvider;

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

    public void setLocalUsersProvider(Supplier<List<String>> provider) {
        this.localUsersProvider = provider;
    }

    public void handleUserSyncMessage(String peerId, String message) {
        try {
            UserSyncInfo syncInfo = UserSyncInfo.fromProtocol(message);

            String action = syncInfo.action();
            List<String> users = syncInfo.connectedUsers();
            Map<String, String> photos = syncInfo.userPhotos();
            String serverPrefix = "Servidor " + peerId.split(":")[0] + " - ";

            if (photos != null && !photos.isEmpty()) {
                lastReceivedPhotos.putAll(photos);
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

    private void notifyClientsAboutRemoteUsers(String peerId, List<String> users, String serverPrefix, String action, List<String> previousUsers) {
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

                        lastReceivedPhotos.remove(username);
                    } else {
                        String photoBase64 = lastReceivedPhotos.getOrDefault(username, "");
                        String msg = protocolParser.encode("USER_CONNECTED", uniqueId, serverPrefix + username, photoBase64);
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
            String photoBase64 = getUserPhoto(username);
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
        Map<String, String> localUsersWithPhotos = getLocalUsersWithPhotos();
        List<String> localUsers = new ArrayList<>(localUsersWithPhotos.keySet());
        String serverId = getServerIdOrDefault();

        UserSyncInfo syncInfo = UserSyncInfo.createFullSync(serverId, localUsers, localUsersWithPhotos);
        String message = syncInfo.toProtocol();

        sendCallback.accept(message);
        logger.info("Sincronización completa de {} usuario(s) enviada a peer {}", localUsers.size(), peerId);
    }

    private List<String> getLocalUsers() {
        if (localUsersProvider != null) {
            return localUsersProvider.get();
        }

        try {
            ActiveUserManager userManager =
                    ActiveUserManager.getInstance();
            return new ArrayList<>(userManager.getAllUserSessions().keySet());
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios locales: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, String> getLocalUsersWithPhotos() {
        Map<String, String> usersWithPhotos = new HashMap<>();
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            Map<String, List<User>> allSessions = userManager.getAllUserSessions();

            for (Map.Entry<String, List<User>> entry : allSessions.entrySet()) {
                String username = entry.getKey();
                List<User> sessions = entry.getValue();

                if (!sessions.isEmpty()) {
                    byte[] photoData = sessions.get(0).getPhotoData();
                    String photoBase64 = "";
                    if (photoData != null && photoData.length > 0) {
                        photoBase64 = java.util.Base64.getEncoder().encodeToString(photoData);
                    }
                    usersWithPhotos.put(username, photoBase64);
                }
            }
        } catch (Exception e) {
            logger.error("Error obteniendo usuarios con fotos: {}", e.getMessage(), e);
        }
        return usersWithPhotos;
    }

    private String getUserPhoto(String username) {
        try {
            ActiveUserManager userManager = ActiveUserManager.getInstance();
            List<User> sessions = userManager.getUserSessions(username);

            if (!sessions.isEmpty()) {
                byte[] photoData = sessions.get(0).getPhotoData();
                if (photoData != null && photoData.length > 0) {
                    return java.util.Base64.getEncoder().encodeToString(photoData);
                }
            }
        } catch (Exception e) {
            logger.error("Error obteniendo foto de usuario {}: {}", username, e.getMessage());
        }
        return "";
    }

    public String generateInitialSyncMessage(int peerPort) {
        Map<String, String> localUsersWithPhotos = getLocalUsersWithPhotos();
        List<String> localUsers = new ArrayList<>(localUsersWithPhotos.keySet());

        String serverId = localServerId;
        if (serverId == null) {
            try {
                serverId = java.net.InetAddress.getLocalHost().getHostAddress() + ":" + peerPort;
            } catch (Exception e) {
                serverId = "localhost:" + peerPort;
            }
        }

        UserSyncInfo syncInfo = UserSyncInfo.createFullSync(serverId, localUsers, localUsersWithPhotos);
        logger.debug("Preparando sincronización inicial: {} usuarios de {}", localUsers.size(), serverId);
        return syncInfo.toProtocol();
    }

    public Map<String, List<String>> getAllUsersAcrossPeers() {
        return new HashMap<>(remoteServerUsers);
    }

    public String getRemoteUserPhoto(String username) {
        return lastReceivedPhotos.getOrDefault(username, "");
    }

    public void clearPeerUsers(String peerId) {
        remoteServerUsers.remove(peerId);
        logger.info("Usuarios de peer {} eliminados", peerId);
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
        } catch (Exception ignore) {}
    }
}
