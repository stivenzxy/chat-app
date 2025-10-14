package com.chatCommon.dto;

import java.io.Serializable;
import java.util.List;

public class GetUsersResponse implements Serializable {
    private final List<UserDTO> users;
    private final boolean success;
    private final String message;

    public GetUsersResponse(boolean success, String message, List<UserDTO> users) {
        this.success = success;
        this.message = message;
        this.users = users;
    }

    public List<UserDTO> getUsers() {
        return users;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}