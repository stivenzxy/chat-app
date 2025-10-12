package com.serverDomain.valueObjects;

import com.serverDomain.exceptions.InvalidDomainException;

public record Username(String value) {
    public Username {
        if (value == null || value.isBlank()) {
            throw new InvalidDomainException("El nombre de usuario no puede estar vacío");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Username username = (Username) obj;
        return value.equals(username.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
