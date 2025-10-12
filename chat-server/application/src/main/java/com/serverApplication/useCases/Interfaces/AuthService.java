package com.serverApplication.useCases.Interfaces;

import com.serverApplication.dto.LoginUserRequest;

public interface AuthService {
    boolean login(LoginUserRequest loginUserRequest);
}
