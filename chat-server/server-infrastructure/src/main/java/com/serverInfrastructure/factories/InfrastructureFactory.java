package com.serverInfrastructure.factories;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.adapters.commands.LoginCommandAdapter;
import com.serverInfrastructure.services.CommandHandler;
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
        LoginCommand loginCommand = new LoginCommand(serviceFactory.createLoginService());
        LoginCommandAdapter loginAdapter = new LoginCommandAdapter(loginCommand);
        handler.registerCommand(loginAdapter);

        // Registrar el nuevo comando de obtener usuarios
        GetAllUsersService getUsersService = serviceFactory.createGetAllUsersService();
        GetUsersCommandAdapter getUsersAdapter = new GetUsersCommandAdapter(getUsersService);
        handler.registerCommand(getUsersAdapter);


        return handler;
    }

    public TcpServer createTcpServer(int port) {
        return new TcpServer(port, createCommandHandler());
    }
}