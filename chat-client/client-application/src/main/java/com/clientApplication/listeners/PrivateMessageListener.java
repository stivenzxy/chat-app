package com.clientApplication.listeners;

import com.clientApplication.events.PrivateMessageEvent;

public interface PrivateMessageListener {
    void onPrivateMessageReceived(PrivateMessageEvent event);
}