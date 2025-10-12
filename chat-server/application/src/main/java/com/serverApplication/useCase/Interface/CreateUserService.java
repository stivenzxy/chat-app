package com.serverApplication.useCase.Interface;

import com.serverApplication.dto.CreateUserRequest;

public interface CreateUserService {
    void execute(CreateUserRequest request);
}