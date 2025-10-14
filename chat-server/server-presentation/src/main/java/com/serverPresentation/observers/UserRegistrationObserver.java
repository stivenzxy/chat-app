package com.serverPresentation.observers;

import com.serverApplication.dto.UserPresentationDTO;

public interface UserRegistrationObserver {
    void onUserRegistered(UserPresentationDTO user);
    void onUserRegistrationError(String error);
    void onUserListUpdated();
}