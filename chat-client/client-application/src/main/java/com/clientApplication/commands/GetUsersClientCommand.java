package com.clientApplication.commands;

import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GetUsersClientCommand implements ClientCommand<Void, GetUsersResponse> {

    private final ServerGatewayPort gateway;

    public GetUsersClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public GetUsersResponse execute(Void request) {
        try {
            List<String> responseParts = gateway.sendAndReceive("GET_USERS");

            boolean success = !responseParts.isEmpty() && "OK".equalsIgnoreCase(responseParts.get(0));
            String message = responseParts.size() > 1 ? responseParts.get(1) : "";

            if (success && responseParts.size() > 2) {
                List<UserDTO> users = parseUsers(responseParts.get(2));
                return new GetUsersResponse(true, message, users);
            } else if (success) {
                return new GetUsersResponse(true, message, Collections.emptyList());
            } else {
                return new GetUsersResponse(false, message, Collections.emptyList());
            }

        } catch (Exception e) {
            return new GetUsersResponse(false, "Error de comunicación: " + e.getMessage(), Collections.emptyList());
        }
    }

    private List<UserDTO> parseUsers(String payload) {
        if (payload == null || payload.isEmpty()) {
            return Collections.emptyList();
        }

        List<UserDTO> users = new ArrayList<>();
        String[] userEntries = payload.split(";");

        for (String entry : userEntries) {
            String[] parts = entry.split(",", 2);
            if (parts.length == 2) {
                users.add(new UserDTO(parts[0], parts[1]));
            }
        }
        return users;
    }
}