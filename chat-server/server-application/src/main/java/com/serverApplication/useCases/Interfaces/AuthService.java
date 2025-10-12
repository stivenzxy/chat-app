package com.serverApplication.useCases.Interfaces;

import com.chatCommon.dto.auth.LoginRequest;

public interface AuthService {
    boolean login(LoginRequest loginUserRequest);
}
