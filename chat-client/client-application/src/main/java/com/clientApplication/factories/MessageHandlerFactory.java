package com.clientApplication.factories;

import com.clientApplication.handlers.AsyncMessageHandler;
import com.clientApplication.handlers.MessageHandler;

public class MessageHandlerFactory {
    private static MessageHandler instance = null;
    
    public static MessageHandler createAsyncMessageHandler() {
        if (instance == null) {
            instance = new AsyncMessageHandler();
        }
        return instance;
    }
}