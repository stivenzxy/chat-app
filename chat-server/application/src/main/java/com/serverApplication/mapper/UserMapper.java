package com.serverApplication.mapper;

import com.serverApplication.dto.CreateUserRequest;
import com.serverDomain.entity.User;
import com.serverDomain.service.PasswordHasher;
import com.serverDomain.valueObject.Email;
import com.serverDomain.valueObject.Username;

public class UserMapper {

    private final PasswordHasher passwordHasher;

    public UserMapper(PasswordHasher passwordHasher) {
        this.passwordHasher = passwordHasher;
    }

    public User toDomain(CreateUserRequest dto) {
        return User.create(
                new Username(dto.getUsername()),
                new Email(dto.getEmail()),
                dto.getPassword(),
                dto.getPhotoUrl(),
                dto.getIpAddress(),
                passwordHasher
        );
    }
}