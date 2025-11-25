package com.serverApplication.useCases;

import com.serverApplication.dto.CreateUserRequest;
import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.ports.UserReplicationNotifier;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;

public class CreateNewUserService implements CreateUserService {
    private final UserRepository userRepository;
    private final CreateUserMapper userMapper;
    private UserReplicationNotifier replicationNotifier;

    public CreateNewUserService(UserRepository userRepository, CreateUserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }
    
    public void setReplicationNotifier(UserReplicationNotifier replicationNotifier) {
        this.replicationNotifier = replicationNotifier;
    }

    @Override
    public void execute(CreateUserRequest request) {
        User user = userMapper.toDomain(request);
        userRepository.save(user);
        
        if (replicationNotifier != null) {
            replicationNotifier.notifyNewUserRegistered(user);
        }
    }
}
