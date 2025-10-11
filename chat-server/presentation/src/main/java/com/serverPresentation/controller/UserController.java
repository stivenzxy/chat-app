package com.serverPresentation.controller;

import com.serverApplication.dto.CreateUserRequest;
import com.serverApplication.useCase.CreateUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserController {
    private final CreateUserService createUserService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(CreateUserService createUserService) {
        this.createUserService = createUserService;
    }

    public void onRegister(String username, String email, String password, String photoUrl, String ip) {
        try {
            var request = new CreateUserRequest(username, email, password, photoUrl, ip);
            createUserService.execute(request);
            logger.info("Usuario creado exitosamente!");
        } catch (Exception e) {
            System.err.println("Error inesperado al crear el usuario: " + e.getMessage());
        }
    }
}