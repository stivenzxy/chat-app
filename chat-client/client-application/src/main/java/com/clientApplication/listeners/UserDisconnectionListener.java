package com.clientApplication.listeners;

import com.clientApplication.events.UserDisconnectionEvent;

public interface UserDisconnectionListener {
    void onUserDisconnected(UserDisconnectionEvent event);
}