package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.observers.ActiveUserManager;
import java.util.Base64;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class GetUsersCommandAdapter implements ProtocolCommandAdapter {

    private final ActiveUserManager activeUserManager = ActiveUserManager.getInstance();
    private final ServerNetworkAdapter serverNetworkAdapter;

    public GetUsersCommandAdapter(ServerNetworkAdapter serverNetworkAdapter) {
        this.serverNetworkAdapter = serverNetworkAdapter;
    }

    @Override
    public String getCommandName() {
        return "GET_USERS";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        try {
            String requesterUserId = connectionContext.getId();

            String requesterUsername = activeUserManager.getActiveUsers().entrySet().stream()
                    .filter(entry -> entry.getValue().getId().equals(requesterUserId))
                    .map(entry -> entry.getKey())
                    .findFirst()
                    .orElse(null);

            List<String> allUsersEntries = new ArrayList<>();
            
            java.util.Set<String> localUsernames = activeUserManager.getActiveUsers().values().stream()
                    .map(u -> u.getUsername().value())
                    .collect(java.util.stream.Collectors.toSet());
            
            activeUserManager.getActiveUsers().values().stream()
                    .filter(user -> requesterUsername == null || !user.getUsername().value().equals(requesterUsername))
                    .forEach(u -> {
                        String photoBase64 = "";
                        if (u.getPhotoData() != null && u.getPhotoData().length > 0) {
                            photoBase64 = Base64.getEncoder().encodeToString(u.getPhotoData());
                        }
                        allUsersEntries.add(u.getId() + "," + u.getUsername().value() + "," + photoBase64);
                    });
            
            if (serverNetworkAdapter != null) {
                Map<String, List<String>> remoteUsers = serverNetworkAdapter.getAllUsersAcrossPeers();
                for (Map.Entry<String, List<String>> entry : remoteUsers.entrySet()) {
                    String serverId = entry.getKey();
                    List<String> usernames = entry.getValue();
                    
                    String serverPrefix = "Servidor " + serverId.split(":")[0] + " - ";
                    
                    for (String username : usernames) {
                        if (!localUsernames.contains(username)) {
                            String photoBase64 = serverNetworkAdapter.getRemoteUserPhoto(username);
                            
                            String uniqueId = serverId + "-" + username;
                            String displayName = serverPrefix + username;
                            allUsersEntries.add(uniqueId + "," + displayName + "," + photoBase64);
                        }
                    }
                }
            }
            
            String usersPayload = String.join(";", allUsersEntries);

            return parser.encode("OK", "Usuarios obtenidos", usersPayload);
        } catch (Exception e) {
            return parser.encode("ERROR", "No se pudieron obtener los usuarios: " + e.getMessage());
        }
    }
}