package com.clientApplication.commands;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

            return new LoginResponse(success, message);
        } catch (Exception exception) {
            return new LoginResponse(false, "Error de comunicación: " + exception.getMessage());
        }
    }
}
