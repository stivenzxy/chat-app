package com.serverPresentation.factories;

import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.useCases.CreateNewUserService;
import com.serverApplication.useCases.Interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.services.PasswordHasher;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.persistence.repository.MysqlUserRepository;
import com.serverInfrastructure.services.BcryptPasswordHasher;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.UserManagementView;

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

    public UserManagementView createUserManagementView() {
        return new UserManagementView(createUserController());
    }

    public TcpServer createTcpServer(int port) {
        LoginService loginService = createLoginService();
        return new TcpServer(port, loginService);
    }
}