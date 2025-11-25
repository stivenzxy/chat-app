package com.serverInfrastructure.factories;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.useCases.CreateNewUserService;
import com.serverApplication.useCases.GetRegisteredUsersService;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.services.PasswordHasher;
import com.serverInfrastructure.persistence.repository.UserManagementRepository;
import com.serverInfrastructure.adapters.peer.replication.UserReplicationNotifierProxy;
import com.serverInfrastructure.services.BcryptPasswordHasher;
import com.serverApplication.useCases.GetAllUsersService;

public class DefaultServiceFactory implements ServiceFactory {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserReplicationNotifierProxy replicationNotifierProxy;

    public DefaultServiceFactory() {
        this.userRepository = new UserManagementRepository();
        this.passwordHasher = new BcryptPasswordHasher();
        this.replicationNotifierProxy = new UserReplicationNotifierProxy();
    }
    
    public UserReplicationNotifierProxy getReplicationNotifierProxy() {
        return replicationNotifierProxy;
    }

    @Override
    public CreateUserService createUserService() {
        CreateNewUserService service = new CreateNewUserService(userRepository, new CreateUserMapper(passwordHasher));
        service.setReplicationNotifier(replicationNotifierProxy);
        return service;
    }

    @Override
    public GetAllUsersService createGetAllUsersService() {
        return new GetAllUsersService(userRepository);
    }

    @Override
    public GetRegisteredUsersService createGetUsersPresentationService() {
        return new GetRegisteredUsersService(userRepository);
    }

    @Override
    public LoginService createLoginService() {
        return new LoginService(userRepository, passwordHasher);
    }
}
