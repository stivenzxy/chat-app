package com.clientApplication.useCases;

import com.clientApplication.dto.LoginRequest;
import com.clientApplication.dto.LoginResponse;
import com.clientApplication.ports.AuthPort;

public class LoginUseCase {
    private final AuthPort authPort;

    public LoginUseCase(AuthPort authPort) {
        this.authPort = authPort;
    }

    public LoginResponse execute(LoginRequest request) {
        return authPort.login(request);
    }
}