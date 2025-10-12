package com.clientApplication.ports;

import com.clientApplication.dto.LoginRequest;
import com.clientApplication.dto.LoginResponse;

public interface AuthPort {
    LoginResponse login(LoginRequest request);
}
