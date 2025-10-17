package com.serverInfrastructure.factories;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.adapters.commands.LoginCommandAdapter;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.adapters.commands.SendPrivateMessageCommandAdapter;
import com.serverInfrastructure.adapters.commands.SendPrivateAudioCommandAdapter;
import com.serverInfrastructure.adapters.commands.GetUsersCommandAdapter;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.persistence.repository.ChannelRepositoryImpl;
import com.serverInfrastructure.adapters.commands.CreateChannelCommandAdapter;
import com.serverInfrastructure.adapters.commands.ListChannelsCommandAdapter;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverInfrastructure.persistence.repository.ChannelInviteRepositoryImpl;
import com.serverInfrastructure.adapters.commands.SendChannelMessageCommandAdapter;
import com.serverInfrastructure.adapters.commands.SendChannelAudioCommandAdapter;
import com.serverInfrastructure.adapters.commands.InviteToChannelCommandAdapter;
import com.serverInfrastructure.adapters.commands.RespondInviteCommandAdapter;
import com.serverInfrastructure.adapters.commands.TranscribeAudioCommandAdapter;
import com.serverInfrastructure.adapters.commands.ListPendingInvitesCommandAdapter;
import com.serverInfrastructure.adapters.commands.GetChannelMembersCommandAdapter;

public class InfrastructureFactory {
    private final ServiceFactory serviceFactory;

    public InfrastructureFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public CommandHandler createCommandHandler() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        CommandHandler handler = new CommandHandler(parser);

    LoginCommandAdapter loginAdapter = new LoginCommandAdapter(serviceFactory.createLoginService());
    loginAdapter.setCommandHandler(handler);
    handler.registerCommand(loginAdapter);

    GetUsersCommandAdapter getUsersAdapter = new GetUsersCommandAdapter();
        handler.registerCommand(getUsersAdapter);

        ChannelRepository channelRepository = new ChannelRepositoryImpl();
        ChannelInviteRepository inviteRepository = new ChannelInviteRepositoryImpl();
        handler.registerCommand(new CreateChannelCommandAdapter(channelRepository));
        handler.registerCommand(new ListChannelsCommandAdapter(channelRepository));
        handler.registerCommand(new SendChannelMessageCommandAdapter(channelRepository, handler));
        handler.registerCommand(new SendChannelAudioCommandAdapter(channelRepository, handler));
        handler.registerCommand(new InviteToChannelCommandAdapter(channelRepository, inviteRepository, handler));
        handler.registerCommand(new RespondInviteCommandAdapter(channelRepository, inviteRepository, handler));
        handler.registerCommand(new ListPendingInvitesCommandAdapter(inviteRepository));
        handler.registerCommand(new GetChannelMembersCommandAdapter(channelRepository));

        TranscribeAudioCommandAdapter transcribeAudioAdapter = new TranscribeAudioCommandAdapter(null);
        handler.registerCommand(transcribeAudioAdapter);

        return handler;
    }

    public TcpServer createTcpServer(int port) {
        CommandHandler handler = createCommandHandler();
        TcpServer server = new TcpServer(port, handler);

        SendPrivateMessageCommandAdapter sendMessageAdapter = new SendPrivateMessageCommandAdapter(server);
        handler.registerCommand(sendMessageAdapter);

        SendPrivateAudioCommandAdapter sendAudioAdapter = new SendPrivateAudioCommandAdapter(server);
        handler.registerCommand(sendAudioAdapter);

        TranscribeAudioCommandAdapter transcribeAudioAdapter = new TranscribeAudioCommandAdapter(server);
        handler.registerCommand(transcribeAudioAdapter);

        return server;
    }
}