package com.serverPresentation.factories;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.useCases.CreateNewUserService;
import com.serverApplication.useCases.Interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.services.PasswordHasher;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.persistence.repository.MysqlUserRepository;
import com.serverInfrastructure.services.BcryptPasswordHasher;
import com.serverInfrastructure.services.CommandHandler;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.MainServerView;

public class ServerFactory {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    public ServerFactory() {
        this.userRepository = new MysqlUserRepository();
        this.passwordHasher = new BcryptPasswordHasher();
    }

    public CreateUserService createUserUseCase() {
        return new CreateNewUserService(userRepository, new CreateUserMapper(passwordHasher));
    }

    public LoginService createLoginService() {
        return new LoginService(userRepository, passwordHasher);
    }

    public UserController createUserController() {
        return new UserController(createUserUseCase());
    }

    public MainServerView createMainServerView() {
        return new MainServerView(this);
    }

    public TcpServer createTcpServer(int port) {
        CommandHandlerFactory commandHandlerFactory = new CommandHandlerFactory(this);
        CommandHandler commandHandler = commandHandlerFactory.create();
        return new TcpServer(port, commandHandler);
    }
}