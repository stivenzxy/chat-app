package com.serverApplication.mappers;

import com.serverApplication.dto.CreateUserRequest;
import com.serverDomain.entities.User;
import com.serverDomain.services.PasswordHasher;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;

public class CreateUserMapper {

    private final PasswordHasher passwordHasher;

    public CreateUserMapper(PasswordHasher passwordHasher) {
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