package com.serverDomain.exception;

public class InvalidDomainException extends IllegalArgumentException {
    public InvalidDomainException(String message) {
        super(message);
    }
}