package com.serverDomain.repository;

import com.serverDomain.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    void deleteById(String id);
}
