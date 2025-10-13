package com.serverApplication.Interfaces.services;

import com.serverApplication.dto.CreateUserRequest;

public interface CreateUserService {
    void execute(CreateUserRequest request);
}