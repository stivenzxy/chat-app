package com.serverInfrastructure.adapters.peer.replication;

import com.serverApplication.ports.UserReplicationNotifier;
import com.serverDomain.entities.User;

public class UserReplicationNotifierProxy implements UserReplicationNotifier {
    private UserReplicationNotifier delegate;

    public void setDelegate(UserReplicationNotifier delegate) {
        this.delegate = delegate;
    }

    @Override
    public void notifyNewUserRegistered(User user) {
        if (delegate != null) {
            delegate.notifyNewUserRegistered(user);
        }
    }
}
