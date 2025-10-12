package com.serverApplication.useCases;

import com.serverApplication.dto.LoginUserRequest;
import com.serverApplication.useCases.Interfaces.AuthService;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.services.PasswordHasher;
import com.serverDomain.valueObjects.Username;

import java.util.Optional;

public class LoginService implements AuthService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;

    public LoginService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public boolean login(LoginUserRequest loginUserRequest) {
        try {
            Username username = new Username(loginUserRequest.getUsername());
            String plainTextPassword = loginUserRequest.getPassword();

            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isEmpty()) {
                return false;
            }

            User user = userOptional.get();
            return user.verifyPassword(plainTextPassword, passwordHasher);

        } catch (Exception exception) {
            return false;
        }
    }
}
