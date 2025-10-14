package com.serverApplication.factories;

import com.serverApplication.useCases.interfaces.CreateUserService;
import com.serverApplication.useCases.LoginService;

public interface ServiceFactory {
    CreateUserService createUserService();
    LoginService createLoginService();
}
