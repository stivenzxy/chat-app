package com.clientApplication.commands;

import com.chatCommon.dto.UserDTO;
import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;
import java.util.Base64;
import java.util.List;

public class LoginClientCommand implements ClientCommand<LoginRequest, LoginResponse> {

    private final ServerGatewayPort gateway;

    public LoginClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public LoginResponse execute(LoginRequest request) {
        try {
            List<String> responseParts = gateway.sendAndReceive(
                    "LOGIN",
                    request.getUsername(),
                    request.getPassword()
            );

            boolean success = !responseParts.isEmpty() && "OK".equalsIgnoreCase(responseParts.get(0));
            String message = responseParts.size() > 1 ? responseParts.get(1) : (success ? "Éxito" : "Respuesta desconocida");


            if (success && responseParts.size() >= 5) {
                String userId = responseParts.get(2);
                String username = responseParts.get(3);
                String photoBase64 = responseParts.get(4);
                byte[] photoData = null;

                if (photoBase64 != null && !photoBase64.isEmpty()) {
                    try {
                        photoData = Base64.getDecoder().decode(photoBase64);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Error parsing photoBase64: " + e.getMessage());
                    }
                }

                UserDTO user = new UserDTO(userId, username, photoData);
                return new LoginResponse(true, message, user);
            } else if (success) {
                return new LoginResponse(true, message, null);
            } else {
                return new LoginResponse(false, message);
            }

        } catch (Exception exception) {
            return new LoginResponse(false, "Error de comunicación: " + exception.getMessage());
        }
    }
}
