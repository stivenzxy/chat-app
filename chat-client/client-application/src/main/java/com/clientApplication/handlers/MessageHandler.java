package com.clientApplication.handlers;

import com.clientApplication.listeners.*;
import java.util.List;

public interface MessageHandler {
    void handleAsyncMessage(List<String> parts);
    void registerUserConnectionListener(UserConnectionListener listener);
    void registerUserDisconnectionListener(UserDisconnectionListener listener);
    void registerPrivateMessageListener(PrivateMessageListener listener);
    void registerPrivateAudioListener(PrivateAudioListener listener);
}