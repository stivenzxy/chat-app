package com.clientApplication.factories;

import com.chatCommon.dto.GetUsersResponse; // Importar
import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.GetUsersClientCommand; // Importar
import com.clientApplication.commands.LoginClientCommand;
import com.clientApplication.commands.contract.ClientCommand;
import com.chatCommon.dto.MessageDTO;
import com.clientApplication.commands.SendPrivateMessageClientCommand;
import com.clientApplication.commands.SendPrivateAudioClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

public class CommandFactory {

    private final ServerGatewayPort gateway;

    public CommandFactory(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    public ClientCommand<LoginRequest, LoginResponse> createLoginCommand() {
        return new LoginClientCommand(gateway);
    }

    public ClientCommand<Void, GetUsersResponse> createGetUsersCommand() {
        return new GetUsersClientCommand(gateway);
    }

    public ClientCommand<MessageDTO, Boolean> createSendPrivateMessageCommand() {
        return new SendPrivateMessageClientCommand(gateway);
    }

    public ClientCommand<MessageDTO, Boolean> createSendPrivateAudioCommand() {
        return new SendPrivateAudioClientCommand(gateway);
    }
}