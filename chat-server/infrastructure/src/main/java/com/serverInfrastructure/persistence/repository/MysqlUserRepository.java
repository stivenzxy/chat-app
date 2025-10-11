package com.serverInfrastructure.persistence.repository;

import com.serverDomain.entity.User;
import com.serverDomain.repository.UserRepository;
import com.serverInfrastructure.persistence.dao.UserDAO;

import java.util.List;
import java.util.Optional;

public class MysqlUserRepository implements UserRepository {

    private final UserDAO userDAO = new UserDAO();

    @Override
    public void save(User user) {
        userDAO.insert(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    @Override
    public Optional<User> findByUsername(String username) {
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
