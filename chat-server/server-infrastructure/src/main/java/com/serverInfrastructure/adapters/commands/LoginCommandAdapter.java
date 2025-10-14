package com.serverInfrastructure.adapters.commands;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;

import java.util.List;

public class LoginCommandAdapter implements ProtocolCommandAdapter {

    private final LoginCommand command;

    public LoginCommandAdapter(LoginCommand command) {
        this.command = command;
    }

    @Override
    public String getCommandName() {
        return "LOGIN";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser) {
        if (parts.size() != 3)
            return parser.encode("ERROR", "Argumentos inválidos para LOGIN");

        LoginRequest request = new LoginRequest(parts.get(1), parts.get(2));
        LoginResponse response = command.execute(request);

        String status = response.isSuccess() ? "OK" : "ERROR";
        return parser.encode(status, response.getMessage());
    }
}