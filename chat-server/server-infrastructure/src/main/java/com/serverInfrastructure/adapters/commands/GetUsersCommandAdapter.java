package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.observers.ActiveUserManager;
import java.util.Base64; // <<< AÑADIR IMPORT
import java.util.List;
import java.util.stream.Collectors;

public class GetUsersCommandAdapter implements ProtocolCommandAdapter {

    private final ActiveUserManager activeUserManager = ActiveUserManager.getInstance();

    public GetUsersCommandAdapter() {}

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

            // --- INICIO DE LA MODIFICACIÓN ---
            // Ahora el payload será: id,username,photo_base64
            String usersPayload = activeUserManager.getActiveUsers().values().stream()
                    .filter(user -> requesterUsername == null || !user.getUsername().value().equals(requesterUsername))
                    .map(u -> {
                        String photoBase64 = "";
                        if (u.getPhotoData() != null && u.getPhotoData().length > 0) {
                            photoBase64 = Base64.getEncoder().encodeToString(u.getPhotoData());
                        }
                        // Unimos las 3 partes
                        return u.getId() + "," + u.getUsername().value() + "," + photoBase64;
                    })
                    .collect(Collectors.joining(";"));
            // --- FIN DE LA MODIFICACIÓN ---

            return parser.encode("OK", "Usuarios obtenidos", usersPayload);
        } catch (Exception e) {
            return parser.encode("ERROR", "No se pudieron obtener los usuarios: " + e.getMessage());
        }
    }
}