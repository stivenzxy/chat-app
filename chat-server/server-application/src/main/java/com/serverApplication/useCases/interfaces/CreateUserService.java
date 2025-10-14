package com.serverApplication.useCases.interfaces;

import com.serverApplication.dto.CreateUserRequest;

public interface CreateUserService {
    void execute(CreateUserRequest request);
}