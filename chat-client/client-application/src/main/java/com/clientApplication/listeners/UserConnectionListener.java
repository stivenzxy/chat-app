package com.clientApplication.listeners;

import com.clientApplication.events.UserConnectionEvent;

public interface UserConnectionListener {
    void onUserConnected(UserConnectionEvent event);
}