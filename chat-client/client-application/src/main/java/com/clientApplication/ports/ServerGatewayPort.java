package com.clientApplication.ports;

import java.util.List;

public interface ServerGatewayPort {
    List<String> sendAndReceive(List<String> requestParts);
}
