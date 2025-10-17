package com.chatCommon.dto.auth;

import com.chatCommon.dto.UserDTO; // <<< AÑADIR IMPORT

public class LoginResponse {
    private final boolean success;
    private final String message;
    private final UserDTO user; // <<< AÑADIR CAMPO

    // --- INICIO DE MODIFICACIÓN DE CONSTRUCTOR ---
    public LoginResponse(boolean success, String message, UserDTO user) {
        this.success = success;
        this.message = message;
        this.user = user;
    }

    public LoginResponse(boolean success, String message) {
        this(success, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public UserDTO getUser() {
        return user;
    }
}