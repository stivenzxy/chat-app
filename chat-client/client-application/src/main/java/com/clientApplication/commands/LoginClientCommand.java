package com.clientApplication.commands;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.Arrays;
import java.util.List;

public class LoginClientCommand implements ClientCommand<LoginRequest, LoginResponse> {

    private final ServerGatewayPort gateway;

    public LoginClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public LoginResponse execute(LoginRequest request) {
        try {
            List<String> requestParts = Arrays.asList("LOGIN", request.getUsername(), request.getPassword());
            List<String> responseParts = gateway.sendAndReceive(requestParts);

            boolean success = !responseParts.isEmpty() && "OK".equalsIgnoreCase(responseParts.get(0));
            String message = responseParts.size() > 1 ? responseParts.get(1) : (success ? "Éxito" : "Respuesta desconocida");

            return new LoginResponse(success, message);
        } catch (Exception e) {
            return new LoginResponse(false, "Error de comunicación: " + e.getMessage());
        }
    }
}
