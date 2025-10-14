package com.chatCommon.dto;

import java.io.Serializable;

public class UserDTO implements Serializable {
    private final String id;
    private final String username;

    public UserDTO(String id, String username) {
        this.id = id;
        this.username = username;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return username;
    }
}