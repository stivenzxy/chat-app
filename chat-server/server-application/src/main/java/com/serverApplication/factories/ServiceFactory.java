package com.serverApplication.factories;

import com.serverApplication.useCases.GetAllUsersService;
import com.serverApplication.useCases.GetRegisteredUsersService;
import com.serverApplication.useCases.GetServerLogsService;
import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;

public interface ServiceFactory {
    CreateUserService createUserService();
    LoginService createLoginService();
    GetAllUsersService createGetAllUsersService(); 
    GetRegisteredUsersService createGetUsersPresentationService(); 
    GetServerLogsService createGetServerLogsService();
}