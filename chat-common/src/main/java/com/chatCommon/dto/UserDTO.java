package com.chatCommon.dto;

import java.io.Serializable;
import java.util.Arrays;

public class UserDTO implements Serializable {
    private final String id;
    private final String username;
    private final byte[] photoData;

    public UserDTO(String id, String username, byte[] photoData) {
        this.id = id;
        this.username = username;
        this.photoData = photoData;
    }

    public UserDTO(String id, String username) {
        this(id, username, null);
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public byte[] getPhotoData() {
        return photoData;
    }

    @Override
    public String toString() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserDTO userDTO = (UserDTO) o;
        return id.equals(userDTO.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}