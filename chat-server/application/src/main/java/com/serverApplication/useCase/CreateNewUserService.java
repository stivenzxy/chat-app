package com.serverApplication.useCase;

import com.serverApplication.dto.CreateUserRequest;
import com.serverApplication.mapper.UserMapper;
import com.serverApplication.useCase.Interface.CreateUserService;
import com.serverDomain.entity.User;
import com.serverDomain.repository.UserRepository;

public class CreateNewUserService implements CreateUserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public CreateNewUserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    @Override
    public void execute(CreateUserRequest request) {
        User user = userMapper.toDomain(request);
        userRepository.save(user);
    }
}
