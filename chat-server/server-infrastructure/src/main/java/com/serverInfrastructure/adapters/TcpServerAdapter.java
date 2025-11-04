package com.serverInfrastructure.adapters;

import com.serverApplication.dto.ConnectedClientInfo;
import com.serverApplication.ports.ClientConnectionObserver;
import com.serverApplication.ports.ServerControl;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.ConnectionListener;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.observers.ActiveUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TcpServerAdapter implements ServerControl, ConnectionListener {

    private final InfrastructureFactory factory;
    private TcpServer server;
    private Thread serverThread;
    private final List<ClientConnectionObserver> appObservers = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(TcpServerAdapter.class);

    public TcpServerAdapter(InfrastructureFactory factory) {
        this.factory = factory;
    }

    @Override
    public void startServer(int port) {
        if (server != null) {
            logger.warn("El servidor ya está en ejecución.");
            return;
        }

        server = factory.createTcpServer(port);
        server.addConnectionListener(this);

        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (RuntimeException e) {
                if (server != null) {
                    logger.error("El hilo del servidor falló inesperadamente.", e);
                } else {
                    logger.debug("Hilo del servidor terminó durante el cierre");
                }
            }
        });
        serverThread.setName("TcpServerThread");
        serverThread.start();
        //logger.info("Adaptador ha iniciado el servidor en el puerto {}.", port);
    }

    @Override
    public void stopServer() {
        if (server != null) {
            logger.info("Adaptador está deteniendo el servidor...");
            server.stop();
            server = null;
        }
        if (serverThread != null) {
            serverThread.interrupt();
            serverThread = null;
        }
        logger.info("Servidor detenido.");
    }

    @Override
    public void addConnectionObserver(ClientConnectionObserver observer) {
        this.appObservers.add(observer);
    }

    public int getMaxConnections() {
        return server != null ? server.getMaxConnections() : 0;
    }

    public int getCurrentConnections() {
        return server != null ? server.getCurrentConnections() : 0;
    }
    
    public void disconnectClient(String clientId) {
        if (server != null) {
            server.disconnectClient(clientId);
        }
    }

    @Override
    public boolean sendBroadcastMessage(String message) {
        if (server == null) {
            logger.warn("No se puede enviar broadcast: servidor no está en ejecución");
            return false;
        }
        
        try {
            String protocolMessage = "SERVER_BROADCAST|" + message;

            server.sendBroadcastMessage(protocolMessage);
            
            logger.info("Mensaje broadcast enviado desde servidor: {}", message);
            return true;
        } catch (Exception e) {
            logger.error("Error al enviar mensaje broadcast: {}", e.getMessage(), e);
            return false;
        }
    }


    @Override
    public void onClientConnected(ClientConnection connection) {
        String username = getUsernameForConnection(connection.getId());
        ConnectedClientInfo clientInfo = new ConnectedClientInfo(
            connection.getPoolSequence(),
            connection.getId(),
            connection.getIpAddress(),
            connection.getReuseCount(),
            username
        );

        for (ClientConnectionObserver observer : appObservers) {
            observer.onClientConnected(clientInfo);
        }
    }

    @Override
    public void onClientDisconnected(ClientConnection connection) {
        String username = getUsernameForConnection(connection.getId());
        ConnectedClientInfo clientInfo = new ConnectedClientInfo(
                connection.getPoolSequence(),
                connection.getId(),
                connection.getIpAddress(),
                connection.getReuseCount(),
                username
        );
        for (ClientConnectionObserver observer : appObservers) {
            observer.onClientDisconnected(clientInfo);
        }
    }

    @Override
    public void onClientIdentityUpdated(ClientConnection connection) {
        String username = getUsernameForConnection(connection.getId());
        ConnectedClientInfo clientInfo = new ConnectedClientInfo(
                connection.getPoolSequence(),
                connection.getId(),
                connection.getIpAddress(),
                connection.getReuseCount(),
                username
        );
        for (ClientConnectionObserver observer : appObservers) {
            observer.onClientDisconnected(clientInfo);
            observer.onClientConnected(clientInfo);
        }
    }
    
    private String getUsernameForConnection(String connectionId) {
        ActiveUserManager activeUserManager = ActiveUserManager.getInstance();

        return activeUserManager.getAllUserSessions().entrySet().stream()
                .filter(entry -> entry.getValue().stream()
                        .anyMatch(user -> user.getId().equals(connectionId)))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
}