package com.serverInfrastructure.network;

import java.net.Socket;

public class ClientConnection {
    private String id;
    private String ipAddress;
    private Socket socket;

    public ClientConnection(String id, String ipAddress, Socket socket) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.socket = socket;
    }

    public void reset(Socket newSocket) {
        this.socket = newSocket;
        this.ipAddress = (newSocket != null) ? newSocket.getInetAddress().getHostAddress() : null;
        this.id = "temp";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Socket getSocket() { return socket; }
    public void setSocket(Socket socket) { this.socket = socket; }

    @Override
    public String toString() {
        return "ClientConnection{" +
                "id='" + id + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", socket=" + socket +
                '}';
    }
}