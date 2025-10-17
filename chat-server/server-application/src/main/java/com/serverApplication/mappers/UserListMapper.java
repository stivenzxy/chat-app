package com.serverApplication.mappers;

import com.serverApplication.dto.UserPresentationDTO;
import com.serverDomain.entities.User;

import java.util.List;
import java.util.stream.Collectors;

public class UserListMapper {
    public static UserPresentationDTO toDTO(User user) {
        if (user == null) {
            return null;
        }

        return new UserPresentationDTO(
                user.getId(),
                user.getUsername().value(),
                user.getEmail().value(),
                user.getIpAddress(),
                user.getCreatedAt(),
                user.getPhotoData()
        );
    }

    public static List<UserPresentationDTO> toDTOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }

        return users.stream()
                .map(UserListMapper::toDTO)
                .collect(Collectors.toList());
    }
}
