package com.serverApplication.dto;

import java.time.LocalDateTime;

/**
 * DTO que representa la información de un servidor peer conectado.
 * 
 * Contiene los datos necesarios para mostrar en la UI y gestionar
 * las conexiones P2P entre servidores.
 */
public record ConnectedPeerInfo(
    String peerId,           // "192.168.1.10:12346"
    String ipAddress,        // "192.168.1.10"
    int port,                // 12346
    String status,           // "Conectado", "Conectando", "Error"
    LocalDateTime connectionTime, // Cuándo se estableció la conexión
    int latency,             // Latencia en ms (-1 si no se ha medido)
    int connectedUsers,      // Número de usuarios en ese servidor
    int availableChannels    // Número de canales en ese servidor
) {
    
    /**
     * Constructor para crear ConnectedPeerInfo con valores por defecto.
     */
    public static ConnectedPeerInfo create(String ipAddress, int port, String status) {
        String peerId = ipAddress + ":" + port;
        return new ConnectedPeerInfo(
            peerId,
            ipAddress,
            port,
            status,
            LocalDateTime.now(),
            -1,  // Latencia no medida
            0,   // Usuarios desconocidos inicialmente
            0    // Canales desconocidos inicialmente
        );
    }
    
    /**
     * Crea una copia con nuevo status.
     */
    public ConnectedPeerInfo withStatus(String newStatus) {
        return new ConnectedPeerInfo(
            peerId, ipAddress, port, newStatus, connectionTime, latency, connectedUsers, availableChannels
        );
    }
    
    /**
     * Crea una copia con nueva latencia.
     */
    public ConnectedPeerInfo withLatency(int newLatency) {
        return new ConnectedPeerInfo(
            peerId, ipAddress, port, status, connectionTime, newLatency, connectedUsers, availableChannels
        );
    }
    
    /**
     * Crea una copia con nuevos contadores de usuarios y canales.
     */
    public ConnectedPeerInfo withCounters(int users, int channels) {
        return new ConnectedPeerInfo(
            peerId, ipAddress, port, status, connectionTime, latency, users, channels
        );
    }
    
    /**
     * Verifica si el peer está en estado conectado.
     */
    public boolean isConnected() {
        return "Conectado".equals(status);
    }
    
    /**
     * Obtiene una representación legible del peer.
     */
    public String getDisplayName() {
        return String.format("%s:%d (%s)", ipAddress, port, status);
    }
}