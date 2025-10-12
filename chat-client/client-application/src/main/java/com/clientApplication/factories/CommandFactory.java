package com.clientApplication.factories;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.LoginClientCommand;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

public class CommandFactory {

    private final ServerGatewayPort gateway;

    public CommandFactory(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    public ClientCommand<LoginRequest, LoginResponse> createLoginCommand() {
        return new LoginClientCommand(gateway);
    }

}