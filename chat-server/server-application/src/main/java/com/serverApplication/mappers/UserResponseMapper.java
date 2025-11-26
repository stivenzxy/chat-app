package com.serverApplication.mappers;

import com.serverApplication.dto.UserPresentationDTO;
import com.serverApplication.dto.UserResponseDTO;

import java.util.List;
import java.util.stream.Collectors;

public class UserResponseMapper {
    
    public static UserResponseDTO toResponseDTO(UserPresentationDTO presentationDTO) {
        return new UserResponseDTO(
                presentationDTO.getId(),
                presentationDTO.getUsername(),
                presentationDTO.getEmail(),
                presentationDTO.getIpAddress(),
                presentationDTO.getCreatedAt()
        );
    }
    
    public static List<UserResponseDTO> toResponseDTOList(List<UserPresentationDTO> presentationDTOs) {
        return presentationDTOs.stream()
                .map(UserResponseMapper::toResponseDTO)
                .collect(Collectors.toList());
    }
}
