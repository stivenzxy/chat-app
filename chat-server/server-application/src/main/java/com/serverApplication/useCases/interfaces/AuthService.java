package com.serverApplication.useCases.interfaces;

import com.chatCommon.dto.auth.LoginRequest;

public interface AuthService {
    boolean login(LoginRequest loginUserRequest);
}
