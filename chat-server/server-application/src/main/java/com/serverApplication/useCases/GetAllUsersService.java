package com.serverApplication.useCases;

import com.chatCommon.dto.UserDTO;
import com.serverDomain.repositories.UserRepository;
import java.util.List;
import java.util.stream.Collectors;

public class GetAllUsersService {

    private final UserRepository userRepository;

    public GetAllUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserDTO> execute() {
        return userRepository.findAll().stream()
                .map(user -> new UserDTO(user.getId(), user.getUsername().value()))
                .collect(Collectors.toList());
    }
}