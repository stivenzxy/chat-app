package com.serverApplication.useCases;

import com.chatCommon.dto.auth.LoginRequest;
import com.serverApplication.useCases.interfaces.AuthService;
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

    public Optional<User> loginAndGetUser(LoginRequest loginUserRequest) {
        try {
            Username username = new Username(loginUserRequest.getUsername());
            String plainTextPassword = loginUserRequest.getPassword();

            Optional<User> userOptional = userRepository.findByUsername(username);

            // Verifica si el usuario existe Y si la contraseña coincide
            if (userOptional.isPresent() && userOptional.get().verifyPassword(plainTextPassword, passwordHasher)) {
                return userOptional; // Éxito, devuelve el usuario
            }

            return Optional.empty(); // Fracaso

        } catch (Exception exception) {
            return Optional.empty();
        }
    }


    @Override
    public boolean login(LoginRequest loginUserRequest) {
        return loginAndGetUser(loginUserRequest).isPresent();
    }
}
