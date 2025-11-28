package com.serverInfrastructure.factories;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.peer.ClientMessageBroadcaster;
import com.serverApplication.ports.peer.PeerMessageRouter;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.adapters.commands.LoginCommandAdapter;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.adapters.commands.SendPrivateMessageCommandAdapter;
import com.serverInfrastructure.adapters.commands.SendPrivateAudioCommandAdapter;
import com.serverInfrastructure.adapters.commands.GetUsersCommandAdapter;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.persistence.repository.ChannelRepositoryImpl;
import com.serverInfrastructure.adapters.commands.CreateChannelCommandAdapter;
import com.serverInfrastructure.adapters.commands.ListChannelsCommandAdapter;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverInfrastructure.persistence.repository.ChannelInviteRepositoryImpl;
import com.serverInfrastructure.adapters.commands.SendChannelMessageCommandAdapter;
import com.serverInfrastructure.adapters.commands.SendChannelAudioCommandAdapter;
import com.serverInfrastructure.persistence.dao.UserDAO;

import java.util.function.Consumer;
import com.serverInfrastructure.adapters.commands.InviteToChannelCommandAdapter;
import com.serverInfrastructure.adapters.commands.RespondInviteCommandAdapter;
import com.serverInfrastructure.adapters.commands.TranscribeAudioCommandAdapter;
import com.serverInfrastructure.adapters.commands.ListPendingInvitesCommandAdapter;
import com.serverInfrastructure.adapters.commands.GetChannelMembersCommandAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfrastructureFactory {
    private final ServiceFactory serviceFactory;
    private ServerNetworkAdapter serverNetworkAdapter;
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureFactory.class);

    public InfrastructureFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public CommandHandler createCommandHandler() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        CommandHandler handler = new CommandHandler(parser);
        UserDAO userDAO = new UserDAO();

        LoginCommandAdapter loginAdapter = new LoginCommandAdapter(serviceFactory.createLoginService());
        loginAdapter.setCommandHandler(handler);
        loginAdapter.setNetworkAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(loginAdapter);

        GetUsersCommandAdapter getUsersAdapter = new GetUsersCommandAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(getUsersAdapter);

        ChannelRepository channelRepository = new ChannelRepositoryImpl();
        ChannelInviteRepository inviteRepository = new ChannelInviteRepositoryImpl();

        CreateChannelCommandAdapter createChannelAdapter = new CreateChannelCommandAdapter(channelRepository);
        createChannelAdapter.setNetworkAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(createChannelAdapter);

        handler.registerCommand(new ListChannelsCommandAdapter(channelRepository));

        SendChannelMessageCommandAdapter sendChannelMsgAdapter = new SendChannelMessageCommandAdapter(channelRepository,
                handler);
        sendChannelMsgAdapter.setNetworkAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(sendChannelMsgAdapter);

        SendChannelAudioCommandAdapter sendChannelAudioAdapter = new SendChannelAudioCommandAdapter(channelRepository,
                handler);
        sendChannelAudioAdapter.setNetworkAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(sendChannelAudioAdapter);

        InviteToChannelCommandAdapter inviteAdapter = new InviteToChannelCommandAdapter(channelRepository,
                inviteRepository, handler, userDAO);
        inviteAdapter.setNetworkAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(inviteAdapter);

        RespondInviteCommandAdapter respondInviteAdapter = new RespondInviteCommandAdapter(channelRepository,
                inviteRepository, handler);
        respondInviteAdapter.setNetworkAdapter(getOrCreateServerNetworkAdapter());
        handler.registerCommand(respondInviteAdapter);

        handler.registerCommand(new ListPendingInvitesCommandAdapter(inviteRepository));
        handler.registerCommand(new GetChannelMembersCommandAdapter(channelRepository));
        handler.registerCommand(
                new com.serverInfrastructure.adapters.commands.GetChannelHistoryCommandAdapter(channelRepository));

        // Note: TranscribeAudioCommandAdapter is registered in createTcpServer() where
        // server instance is available

        return handler;
    }

    public TcpServer createTcpServer() {
        CommandHandler handler = createCommandHandler();
        TcpServer server = new TcpServer(handler);

        try {
            ServerNetworkAdapter networkAdapter = getOrCreateServerNetworkAdapter();

            networkAdapter.registerClientBroadcast(server::sendDirectBroadcast);

            ClientMessageBroadcaster targetedBroadcaster = (targetUsername, message, excludeConnectionId) -> {
                if (targetUsername == null || targetUsername.isBlank()) {
                    server.sendDirectBroadcast(message);
                    return;
                }

                if (excludeConnectionId != null && !excludeConnectionId.isBlank()) {
                    server.sendMessageToUserExceptSession(targetUsername, message, excludeConnectionId, null);
                } else {
                    server.sendMessageToUser(targetUsername, message, null);
                }
            };

            networkAdapter.registerClientMessageBroadcaster(targetedBroadcaster);
        } catch (Exception e) {
            logger.info("No se pudo registrar callbacks de broadcast P2P: {}", e.getMessage());
        }

        SendPrivateMessageCommandAdapter sendMessageAdapter = new SendPrivateMessageCommandAdapter(server);

        try {
            ServerNetworkAdapter networkAdapter = getOrCreateServerNetworkAdapter();
            sendMessageAdapter.setPeerRoutingCallback(networkAdapter::routePrivateMessageToPeer);
            logger.info("Callback de enrutamiento P2P configurado en SendPrivateMessageCommandAdapter");
        } catch (Exception e) {
            logger.warn("No se pudo configurar callback de enrutamiento P2P: {}", e.getMessage());
        }

        handler.registerCommand(sendMessageAdapter);

        SendPrivateAudioCommandAdapter sendAudioAdapter = new SendPrivateAudioCommandAdapter(server);

        try {
            ServerNetworkAdapter networkAdapter = getOrCreateServerNetworkAdapter();

            PeerMessageRouter router = networkAdapter::routePrivateAudioToPeer;

            sendAudioAdapter.setPeerMessageRouter(router);
            System.out.println("[INFO] PeerMessageRouter configurado en SendPrivateAudioCommandAdapter");
        } catch (Exception e) {
            System.out.println("[WARN] No se pudo configurar PeerMessageRouter para audio: " + e.getMessage());
        }

        handler.registerCommand(sendAudioAdapter);

        // TranscribeAudioCommandAdapter needs the server instance, so it's registered
        // here
        TranscribeAudioCommandAdapter transcribeAudioAdapter = new TranscribeAudioCommandAdapter(server);
        handler.registerCommand(transcribeAudioAdapter);

        return server;
    }

    private Consumer<Void> userReplicationCallback;

    private ServerNetworkAdapter getOrCreateServerNetworkAdapter() {
        if (serverNetworkAdapter == null) {
            serverNetworkAdapter = new ServerNetworkAdapter(userReplicationCallback);
        }
        return serverNetworkAdapter;
    }

    public ServerNetworkAdapter createServerNetworkAdapter() {
        return getOrCreateServerNetworkAdapter();
    }

    /**
     * Sets callback to be invoked when users are replicated from other peers.
     * Must be called before createServerNetworkAdapter().
     */
    public void setUserReplicationCallback(Consumer<Void> callback) {
        if (serverNetworkAdapter != null) {
            throw new IllegalStateException("Cannot set callback after ServerNetworkAdapter is created");
        }
        this.userReplicationCallback = callback;
    }
}