package com.serverInfrastructure.network;

import java.net.Socket;

public record ClientConnection(String id, String ipAddress, Socket socket) {
}