package com.serverDomain.valueObjects;

import com.serverDomain.exceptions.InvalidDomainException;

import java.util.regex.Pattern;

public record Email(String value) {
    public Email {
        if (value == null || !Pattern.matches(".+@.+\\..+", value)) {
            throw new InvalidDomainException("Formato de email inválido");
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Email userEmail = (Email) obj;
        return value.equals(userEmail.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
