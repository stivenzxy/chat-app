package com.serverDomain.exceptions;

public class InvalidDomainException extends IllegalArgumentException {
    public InvalidDomainException(String message) {
        super(message);
    }
}