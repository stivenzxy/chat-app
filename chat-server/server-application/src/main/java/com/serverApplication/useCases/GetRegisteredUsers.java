package com.serverApplication.useCases;

import com.serverApplication.dto.UserPresentationDTO;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

public class GetRegisteredUsers {
    
    private final UserRepository userRepository;

    public GetRegisteredUsers(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    

    public List<UserPresentationDTO> getAllUsersForPresentation() {
        List<User> domainUsers = userRepository.findAll();
        return mapToPresentationDTOs(domainUsers);
    }
    

    private UserPresentationDTO mapToDTO(User user) {
        if (user == null) {
            return null;
        }
        
        return new UserPresentationDTO(
            user.getId(),
            user.getUsername().value(),
            user.getEmail().value(),
            user.getIpAddress(),
            user.getCreatedAt(),
            user.getPhotoUrl()
        );
    }

    private List<UserPresentationDTO> mapToPresentationDTOs(List<User> users) {
        if (users == null) {
            return List.of();
        }
        
        return users.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}