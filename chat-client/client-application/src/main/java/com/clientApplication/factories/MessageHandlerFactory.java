package com.clientApplication.factories;

import com.clientApplication.handlers.AsyncMessageHandler;
import com.clientApplication.handlers.MessageHandler;

public class MessageHandlerFactory {
    public static MessageHandler createAsyncMessageHandler() {
        return new AsyncMessageHandler();
    }
}