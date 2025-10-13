package com.serverApplication.Interfaces.services;

import com.chatCommon.dto.auth.LoginRequest;

public interface AuthService {
    boolean login(LoginRequest loginUserRequest);
}
