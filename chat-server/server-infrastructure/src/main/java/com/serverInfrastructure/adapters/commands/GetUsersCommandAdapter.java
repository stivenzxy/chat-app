package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.ActiveUserManager;
import java.util.List;
import java.util.stream.Collectors;

public class GetUsersCommandAdapter implements ProtocolCommandAdapter {

    private final ActiveUserManager activeUserManager = ActiveUserManager.getInstance();

    // Ya no necesita GetAllUsersService, puedes eliminarlo del constructor
    public GetUsersCommandAdapter() {}

    @Override
    public String getCommandName() {
        return "GET_USERS";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        try {
            // Obtener el ID del usuario que hace la petición
            String requesterId = connectionContext.getId();

            // Obtener todos los usuarios activos, filtrarlos para no incluir al solicitante
            String usersPayload = activeUserManager.getActiveUsers().values().stream()
                    .filter(user -> !user.getUsername().value().equals(requesterId))
                    .map(u -> u.getId() + "," + u.getUsername().value())
                    .collect(Collectors.joining(";"));

            return parser.encode("OK", "Usuarios obtenidos", usersPayload);
        } catch (Exception e) {
            return parser.encode("ERROR", "No se pudieron obtener los usuarios: " + e.getMessage());
        }
    }
}