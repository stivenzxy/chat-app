package com.chatCommon.dto.auth;

import com.chatCommon.dto.UserDTO;

public class LoginResponse {
    private final boolean success;
    private final String message;
    private final UserDTO user;

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