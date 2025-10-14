package com.clientApplication.ports;

import java.util.List;
import java.util.Map;

public interface ServerGatewayPort {
    List<String> sendAndReceive(String command, String... args);
}
