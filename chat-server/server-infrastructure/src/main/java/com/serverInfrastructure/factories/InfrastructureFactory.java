package com.serverInfrastructure.factories;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.adapters.commands.LoginCommandAdapter;
import com.serverInfrastructure.services.CommandHandler;

public class InfrastructureFactory {
    private final ServiceFactory serviceFactory;

    public InfrastructureFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public CommandHandler createCommandHandler() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        CommandHandler handler = new CommandHandler(parser);

        LoginCommand loginCommand = new LoginCommand(serviceFactory.createLoginService());
        LoginCommandAdapter loginAdapter = new LoginCommandAdapter(loginCommand);
        handler.registerCommand(loginAdapter);

        return handler;
    }

    public TcpServer createTcpServer(int port) {
        return new TcpServer(port, createCommandHandler());
    }
}