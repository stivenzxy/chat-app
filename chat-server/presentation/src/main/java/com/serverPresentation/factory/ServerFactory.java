package com.serverPresentation.factory;

import com.serverApplication.mapper.UserMapper;
import com.serverApplication.useCase.CreateNewUserService;
import com.serverApplication.useCase.Interface.CreateUserService;
import com.serverDomain.repository.UserRepository;
import com.serverDomain.service.PasswordHasher;
import com.serverInfrastructure.persistence.repository.MysqlUserRepository;
import com.serverInfrastructure.service.BcryptPasswordHasher;
import com.serverPresentation.controller.UserController;
import com.serverPresentation.view.UserManagementView;

public class ServerFactory {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public ServerFactory() {
        this.userRepository = new MysqlUserRepository();
        this.passwordHasher = new BcryptPasswordHasher();
    }

    public CreateUserService createUserUseCase() {
        return new CreateNewUserService(userRepository, new UserMapper(passwordHasher));
    }

    public UserController createUserController() {
        return new UserController(createUserUseCase());
    }

    public UserManagementView createUserManagementView() {
        return new UserManagementView(createUserController());
    }
}