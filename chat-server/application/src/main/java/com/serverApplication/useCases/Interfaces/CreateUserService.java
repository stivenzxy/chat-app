package com.serverApplication.useCases.Interfaces;

import com.serverApplication.dto.CreateUserRequest;

public interface CreateUserService {
    void execute(CreateUserRequest request);
}