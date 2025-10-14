package com.clientApplication.ports;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ServerGatewayPort {
    List<String> sendAndReceive(String command, String... args);
    void setAsyncMessageListener(Consumer<List<String>> listener);
    void setDisconnectListener(Consumer<String> listener);
}
