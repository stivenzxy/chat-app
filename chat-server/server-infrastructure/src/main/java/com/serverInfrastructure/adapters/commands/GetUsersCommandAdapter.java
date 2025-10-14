package com.serverInfrastructure.adapters.commands;

import com.chatCommon.dto.UserDTO;
import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.useCases.GetAllUsersService;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;

import java.util.List;
import java.util.stream.Collectors;

public class GetUsersCommandAdapter implements ProtocolCommandAdapter {

    private final GetAllUsersService getAllUsersService;

    public GetUsersCommandAdapter(GetAllUsersService getAllUsersService) {
        this.getAllUsersService = getAllUsersService;
    }

    @Override
    public String getCommandName() {
        return "GET_USERS";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser) {
        try {
            List<UserDTO> users = getAllUsersService.execute();

            // Formato: OK|id1,user1;id2,user2;...
            String usersPayload = users.stream()
                    .map(u -> u.getId() + "," + u.getUsername())
                    .collect(Collectors.joining(";"));

            return parser.encode("OK", "Usuarios obtenidos", usersPayload);
        } catch (Exception e) {
            return parser.encode("ERROR", "No se pudieron obtener los usuarios: " + e.getMessage());
        }
    }
}