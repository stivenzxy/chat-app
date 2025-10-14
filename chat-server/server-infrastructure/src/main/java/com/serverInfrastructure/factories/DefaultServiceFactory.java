package com.serverInfrastructure.factories;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.useCases.CreateNewUserService;
import com.serverApplication.useCases.GetRegisteredUsers;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.services.PasswordHasher;
import com.serverInfrastructure.persistence.repository.UserManagementRepository;
import com.serverInfrastructure.services.BcryptPasswordHasher;
import com.serverApplication.useCases.GetAllUsersService;

public class DefaultServiceFactory implements ServiceFactory {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public DefaultServiceFactory() {
        this.userRepository = new UserManagementRepository();
        this.passwordHasher = new BcryptPasswordHasher();
    }

    @Override
    public CreateUserService createUserService() {
        return new CreateNewUserService(userRepository, new CreateUserMapper(passwordHasher));
    }

    @Override
    public GetAllUsersService createGetAllUsersService() {
        return new GetAllUsersService(userRepository);
    }

    @Override
    public GetRegisteredUsers createGetUsersPresentationService() {
        return new GetRegisteredUsers(userRepository);
    }

    @Override
    public LoginService createLoginService() {
        return new LoginService(userRepository, passwordHasher);
    }
}
