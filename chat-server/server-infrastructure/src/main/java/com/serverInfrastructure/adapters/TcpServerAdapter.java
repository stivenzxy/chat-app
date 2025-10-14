package com.serverInfrastructure.adapters;

import com.serverApplication.dto.ConnectedClientInfo;
import com.serverApplication.ports.ClientConnectionObserver;
import com.serverApplication.ports.ServerControl;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.ConnectionListener;
import com.serverInfrastructure.network.TcpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
            } catch (Exception e) {
                logger.error("El hilo del servidor falló inesperadamente.", e);
            }
        });
        serverThread.setName("TcpServerThread");
        serverThread.start();
        logger.info("Adaptador ha iniciado el servidor en el puerto {}.", port);
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


    @Override
    public void onClientConnected(ClientConnection connection) {
        logger.debug("Evento de red 'conectado' recibido para {}. Traduciendo para la capa de aplicación.", connection.getId());

        ConnectedClientInfo clientInfo = new ConnectedClientInfo(connection.getId(), connection.getIpAddress());

        for (ClientConnectionObserver observer : appObservers) {
            observer.onClientConnected(clientInfo);
        }
    }

    @Override
    public void onClientDisconnected(ClientConnection connection) {
        logger.debug("Evento de red 'desconectado' recibido para {}. Traduciendo para la capa de aplicación.", connection.getId());
        ConnectedClientInfo clientInfo = new ConnectedClientInfo(connection.getId(), connection.getIpAddress());
        for (ClientConnectionObserver observer : appObservers) {
            observer.onClientDisconnected(clientInfo);
        }
    }
}