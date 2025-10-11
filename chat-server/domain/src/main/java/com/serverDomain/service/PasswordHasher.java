package com.serverDomain.service;

public interface PasswordHasher {
    String hash(String password);
    boolean check(String password, String hashedPassword);
}