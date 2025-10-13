package com.serverPresentation.factories;


import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverApplication.useCases.LoginService;
import com.serverInfrastructure.services.CommandHandler;

public class CommandHandlerFactory {

    private final ServerFactory coreFactory;

    public CommandHandlerFactory(ServerFactory coreFactory) {
        this.coreFactory = coreFactory;
    }

    public CommandHandler create() {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        CommandHandler handler = new CommandHandler(parser);

        LoginService loginService = coreFactory.createLoginService();

        handler.registerCommand("LOGIN", new LoginCommand(loginService));
        // handler.registerCommand("SEND_MESSAGE", new SendMessageCommand(coreFactory.createMessageService()));

        return handler;
    }
}