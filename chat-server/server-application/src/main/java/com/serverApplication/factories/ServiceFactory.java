package com.serverApplication.factories;

import com.serverApplication.useCases.GetAllUsersService; // Importar
import com.serverApplication.useCases.GetRegisteredUsers;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;

public interface ServiceFactory {
    CreateUserService createUserService();
    LoginService createLoginService();
    GetAllUsersService createGetAllUsersService(); // Añadir esta línea
    GetRegisteredUsers createGetUsersPresentationService(); // Nuevo servicio
}