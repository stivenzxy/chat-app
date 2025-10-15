package com.serverInfrastructure.network;

import java.net.Socket;

public class ClientConnection {
    private String id;
    private String ipAddress;
    private Socket socket;
    // Número de veces que esta instancia ha sido reutilizada después de su creación inicial
    private int reuseCount = 0;
    // Identificador de secuencia asignado cuando la instancia se creó en el pool (permite diferenciar instancias reutilizadas)
    private long poolSequence;

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
    
    public void resetForReuse(Socket newSocket) {
        this.socket = newSocket;
        this.ipAddress = (newSocket != null) ? newSocket.getInetAddress().getHostAddress() : null;
        this.id = "temp";
    }
    
    public void clearSocketOnly() {
        // Solo cierra el socket pero mantiene el ID para logging
        this.socket = null;
        this.ipAddress = null;
        // NO cambiamos el ID aquí
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public Socket getSocket() { return socket; }
    public void setSocket(Socket socket) { this.socket = socket; }

    public int getReuseCount() { return reuseCount; }
    public void incrementReuseCount() { this.reuseCount++; }
    public void setPoolSequence(long poolSequence) { this.poolSequence = poolSequence; }
    public long getPoolSequence() { return poolSequence; }

    @Override
    public String toString() {
        return "ClientConnection{" +
                "id='" + id + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", socket=" + (socket != null) +
                ", reuseCount=" + reuseCount +
                ", poolSeq=" + poolSequence +
                '}';
    }
}