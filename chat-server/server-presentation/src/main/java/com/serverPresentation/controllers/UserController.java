package com.serverPresentation.controllers;

import com.serverApplication.dto.CreateUserRequest;
import com.serverApplication.useCases.interfaces.CreateUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserController {
    private final CreateUserService createUserService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(CreateUserService createUserService) {
        this.createUserService = createUserService;
    }

    public void onRegister(CreateUserRequest createUserRequest) {
        try {
            var request = new CreateUserRequest(
                    createUserRequest.getUsername(),
                    createUserRequest.getEmail(),
                    createUserRequest.getPassword(),
                    createUserRequest.getPhotoUrl(),
                    createUserRequest.getIpAddress()
            );
            createUserService.execute(request);
            logger.info("Usuario creado exitosamente!");
        } catch (Exception e) {
            logger.error("Error inesperado al crear el usuario: {}", e.getMessage());
        }
    }
}