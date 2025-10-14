package com.serverInfrastructure.factories;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.adapters.commands.LoginCommandAdapter;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.adapters.commands.SendPrivateMessageCommandAdapter;
import com.serverInfrastructure.adapters.commands.SendPrivateAudioCommandAdapter;
import com.serverApplication.useCases.GetAllUsersService;
import com.serverInfrastructure.adapters.commands.GetUsersCommandAdapter;

public class InfrastructureFactory {
    private final ServiceFactory serviceFactory;

    public InfrastructureFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public CommandHandler createCommandHandler() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        CommandHandler handler = new CommandHandler(parser);

        // Comando de Login existente
        LoginCommandAdapter loginAdapter = new LoginCommandAdapter(serviceFactory.createLoginService());
        handler.registerCommand(loginAdapter);

        // Registrar el nuevo comando de obtener usuarios
        GetAllUsersService getUsersService = serviceFactory.createGetAllUsersService();
        GetUsersCommandAdapter getUsersAdapter = new GetUsersCommandAdapter();
        handler.registerCommand(getUsersAdapter);

        return handler;
    }

    public TcpServer createTcpServer(int port) {
        CommandHandler handler = createCommandHandler();
        TcpServer server = new TcpServer(port, handler);

        // Adaptador de mensajes de texto
        SendPrivateMessageCommandAdapter sendMessageAdapter = new SendPrivateMessageCommandAdapter(server);
        handler.registerCommand(sendMessageAdapter);

        // NUEVO: Adaptador de mensajes de audio
        SendPrivateAudioCommandAdapter sendAudioAdapter = new SendPrivateAudioCommandAdapter(server);
        handler.registerCommand(sendAudioAdapter);

        return server;
    }
}