package com.serverApplication.useCases;

import com.serverApplication.dto.CreateUserRequest;
import com.serverApplication.mappers.CreateUserMapper;
import com.serverApplication.useCases.Interfaces.CreateUserService;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;

public class CreateNewUserService implements CreateUserService {
    private final UserRepository userRepository;
    private final CreateUserMapper userMapper;

    public CreateNewUserService(UserRepository userRepository, CreateUserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public void execute(CreateUserRequest request) {
        User user = userMapper.toDomain(request);
        userRepository.save(user);
    }
}
