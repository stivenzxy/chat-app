package com.serverPresentation.controllers;

import com.serverApplication.dto.CreateUserRequest;
import com.serverApplication.dto.UserPresentationDTO;
import com.serverApplication.useCases.GetRegisteredUsersService;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverPresentation.observers.UserRegistrationObservable;
import com.serverPresentation.observers.UserRegistrationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UserController {
    private final CreateUserService createUserService;
    private final GetRegisteredUsersService getUsersPresentationService;
    private final UserRegistrationObservable observable;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(CreateUserService createUserService, GetRegisteredUsersService getUsersPresentationService) {
        this.createUserService = createUserService;
        this.getUsersPresentationService = getUsersPresentationService;
        this.observable = new UserRegistrationObservable();
    }

    public void onRegister(CreateUserRequest createUserRequest) {
        try {
            logger.info("Registrando usuario usando controller: {}", this.hashCode());
            var request = new CreateUserRequest(
                    createUserRequest.getUsername(),
                    createUserRequest.getEmail(),
                    createUserRequest.getPassword(),
                    createUserRequest.getPhotoData(),
                    createUserRequest.getIpAddress()
            );
            createUserService.execute(request);
            logger.info("Usuario creado exitosamente!");

            //logger.info("Notificando a {} observers", observable.getObserverCount());
            observable.notifyUserRegistered(null);
        } catch (Exception e) {
            logger.error("Error inesperado al crear el usuario: {}", e.getMessage());
            observable.notifyUserRegistrationError(e.getMessage());
        }
    }
    
    public List<UserPresentationDTO> getAllUsers() {
        try {
            List<UserPresentationDTO> users = getUsersPresentationService.getAllUsersForPresentation();
            observable.notifyUserListUpdated();
            return users;
        } catch (Exception e) {
            logger.error("Error al obtener todos los usuarios: {}", e.getMessage());
            throw new RuntimeException("Error al cargar usuarios", e);
        }
    }
    
    public void addUserRegistrationObserver(UserRegistrationObserver observer) {
        observable.addObserver(observer);
    }
}