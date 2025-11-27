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
import com.clientApplication.commands.CreateChannelClientCommand;
import com.clientApplication.commands.ListChannelsClientCommand;
import com.clientApplication.commands.SendChannelMessageClientCommand;
import com.clientApplication.commands.SendChannelAudioClientCommand;
import com.clientApplication.commands.InviteToChannelClientCommand;
import com.clientApplication.commands.RespondInviteClientCommand;
import com.clientApplication.commands.ListPendingInvitesClientCommand;
import com.clientApplication.commands.TranscribeAudioClientCommand;
import com.clientApplication.commands.GetChannelMembersClientCommand;
import com.clientApplication.commands.GetChannelHistoryClientCommand;
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

    public CreateChannelClientCommand createCreateChannelCommand() {
        return new CreateChannelClientCommand(gateway);
    }

    public ListChannelsClientCommand createListChannelsCommand() {
        return new ListChannelsClientCommand(gateway);
    }

    public SendChannelMessageClientCommand createSendChannelMessageCommand() {
        return new SendChannelMessageClientCommand(gateway);
    }

    public SendChannelAudioClientCommand createSendChannelAudioCommand() {
        return new SendChannelAudioClientCommand(gateway);
    }

    public InviteToChannelClientCommand createInviteToChannelCommand() {
        return new InviteToChannelClientCommand(gateway);
    }

    public RespondInviteClientCommand createRespondInviteCommand() {
        return new RespondInviteClientCommand(gateway);
    }

    public ListPendingInvitesClientCommand createListPendingInvitesCommand() {
        return new ListPendingInvitesClientCommand(gateway);
    }

    public GetChannelMembersClientCommand createGetChannelMembersCommand() {
        return new GetChannelMembersClientCommand(gateway);
    }

    public GetChannelHistoryClientCommand createGetChannelHistoryCommand() {
        return new GetChannelHistoryClientCommand(gateway);
    }

    public TranscribeAudioClientCommand createTranscribeAudioCommand() {
        return new TranscribeAudioClientCommand(gateway);
    }
}