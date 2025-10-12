package com.clientInfrastructure.adapters;

import com.clientApplication.dto.LoginRequest;
import com.clientApplication.dto.LoginResponse;
import com.clientApplication.ports.AuthPort;
import com.clientInfrastructure.network.TcpClient;
import com.protocol.ProtocolParser;

import java.util.List;

public class TcpAuthAdapter implements AuthPort {
    private final TcpClient tcpClient;
    private final ProtocolParser parser;

    public TcpAuthAdapter(TcpClient tcpClient, ProtocolParser parser) {
        this.tcpClient = tcpClient;
        this.parser = parser;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try {
            String messageToSend = parser.encode("LOGIN", request.getUsername(), request.getPassword());
            String rawResponse = tcpClient.sendMessage(messageToSend);

            if (rawResponse == null) {
                return new LoginResponse(false, "No se recibió respuesta del servidor.");
            }

            List<String> responseParts = parser.decode(rawResponse);
            boolean success = !responseParts.isEmpty() && "OK".equalsIgnoreCase(responseParts.get(0));
            String message = responseParts.size() > 1 ? responseParts.get(1) : (success ? "" : "Respuesta inválida");

            return new LoginResponse(success, message);
        } catch (Exception e) {
            return new LoginResponse(false, "Error de comunicación: " + e.getMessage());
        }
    }
}