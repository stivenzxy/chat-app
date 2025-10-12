package com.serverApplication.commands;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.serverApplication.commands.contract.Command;
import com.serverApplication.useCases.LoginService;

public class LoginCommand implements Command<LoginRequest, LoginResponse> {
    private final LoginService loginService;

    public LoginCommand(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public LoginResponse execute(LoginRequest request) {
        boolean success = loginService.login(request);
        String message = success ? "Login exitoso" : "Credenciales inválidas";
        return new LoginResponse(success, message);
    }
}