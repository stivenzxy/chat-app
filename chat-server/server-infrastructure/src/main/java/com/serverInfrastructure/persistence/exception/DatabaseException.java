package com.serverInfrastructure.persistence.exception;

public class DatabaseException extends RuntimeException {
    private final int statusCode;

    public DatabaseException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}