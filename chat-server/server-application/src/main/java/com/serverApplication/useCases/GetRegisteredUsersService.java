package com.serverApplication.useCases;

import com.serverApplication.dto.UserPresentationDTO;
import com.serverApplication.mappers.UserListMapper;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;

import java.util.List;

public class GetRegisteredUsersService {

    private final UserRepository userRepository;

    public GetRegisteredUsersService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserPresentationDTO> getAllUsersForPresentation() {
        List<User> domainUsers = userRepository.findAll();
        return UserListMapper.toDTOList(domainUsers);
    }
}