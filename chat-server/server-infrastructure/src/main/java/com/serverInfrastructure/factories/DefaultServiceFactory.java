package com.serverInfrastructure.factories;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.ports.LogReader;
import com.serverApplication.useCases.CreateNewUserService;
import com.serverApplication.useCases.GetRegisteredUsersService;
import com.serverApplication.useCases.GetServerLogsService;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.services.PasswordHasher;
import com.serverInfrastructure.adapters.LogFileReaderAdapter;
import com.serverInfrastructure.persistence.repository.UserManagementRepository;
import com.serverInfrastructure.adapters.peer.replication.UserReplicationNotifierProxy;
import com.serverInfrastructure.services.BcryptPasswordHasher;
import com.serverApplication.useCases.GetAllUsersService;

public class DefaultServiceFactory implements ServiceFactory {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserReplicationNotifierProxy replicationNotifierProxy;
    private final LogReader logReader;

    public DefaultServiceFactory() {
        this.userRepository = new UserManagementRepository();
        this.passwordHasher = new BcryptPasswordHasher();
        this.replicationNotifierProxy = new UserReplicationNotifierProxy();
        this.logReader = new LogFileReaderAdapter(); // Uses default "server.log"
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

    @Override
    public GetServerLogsService createGetServerLogsService() {
        return new GetServerLogsService(logReader);
    }
}
