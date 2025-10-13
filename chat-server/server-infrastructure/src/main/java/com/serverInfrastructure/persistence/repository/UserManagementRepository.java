package com.serverInfrastructure.persistence.repository;

import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;
import com.serverInfrastructure.persistence.dao.UserDAO;

import java.util.List;
import java.util.Optional;

public class UserManagementRepository implements UserRepository {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void save(User user) {
        userDAO.insert(user);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return userDAO.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(Username username) {
        return userDAO.findByUsername(username);
    }

    @Override
    public List<User> findAll() {
        return userDAO.selectAll();
    }

    @Override
    public void deleteById(String id) {
        userDAO.deleteById(id);
    }
}
