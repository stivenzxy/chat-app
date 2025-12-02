package com.serverDomain.repositories;

import com.serverDomain.entities.User;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    void save(User user);
    Optional<User> findByEmail(Email email);
    Optional<User> findByUsername(Username username);
    List<User> findAll();
    void deleteById(String id);
 
    void saveReplicatedUser(User user);
    void updateReplicatedUsersTimestamp(String originServerId);
}
