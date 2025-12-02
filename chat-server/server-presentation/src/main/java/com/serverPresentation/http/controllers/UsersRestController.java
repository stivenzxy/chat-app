package com.serverPresentation.http.controllers;

import com.serverApplication.dto.UserPresentationDTO;
import com.serverApplication.dto.UserResponseDTO;
import com.serverApplication.mappers.UserResponseMapper;
import com.serverApplication.useCases.GetRegisteredUsersService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class UsersRestController {
    private static final Logger logger = LoggerFactory.getLogger(UsersRestController.class);
    private final GetRegisteredUsersService getRegisteredUsersService;

    public UsersRestController(GetRegisteredUsersService getRegisteredUsersService) {
        this.getRegisteredUsersService = getRegisteredUsersService;
    }

    public void getUsers(Context ctx) {
        try {
            List<UserPresentationDTO> presentationDTOs = getRegisteredUsersService.getAllUsersForPresentation();
            List<UserResponseDTO> responseDTOs = UserResponseMapper.toResponseDTOList(presentationDTOs);
            
            ctx.json(responseDTOs);
            logger.debug("Returned {} users", responseDTOs.size());
        } catch (Exception e) {
            logger.error("Error retrieving users: {}", e.getMessage(), e);
            ctx.status(500).json(new ErrorResponse("Error retrieving users"));
        }
    }

    private record ErrorResponse(String error) {}
}
