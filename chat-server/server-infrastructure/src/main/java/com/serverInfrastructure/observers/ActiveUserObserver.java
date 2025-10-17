package com.serverInfrastructure.observers;

import com.serverDomain.entities.User;

public interface ActiveUserObserver {
    void onUserLoggedIn(User user);
    void onUserLoggedOut(User user);
}