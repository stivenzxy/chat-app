package com.serverApplication.ports;

import com.serverDomain.entities.User;

public interface UserReplicationNotifier {
    void notifyNewUserRegistered(User user);
}
