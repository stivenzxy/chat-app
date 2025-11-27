package com.serverPresentation;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.adapters.TcpServerAdapter;
import com.serverInfrastructure.factories.DefaultServiceFactory;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.factories.PresentationFactory;
import com.serverPresentation.http.HttpRestServer;
import com.serverPresentation.views.MainServerView;

import javax.swing.*;

public class ServerApplication {
    public static void main(String[] args) {
        DefaultServiceFactory serviceFactory = new DefaultServiceFactory();
        InfrastructureFactory infraFactory = new InfrastructureFactory(serviceFactory);
        TcpServerAdapter serverControl = new TcpServerAdapter(infraFactory);

        PresentationFactory presentationFactory = new PresentationFactory(
            serviceFactory, 
            infraFactory, 
            serverControl,
            null
        );

        UserController userController = presentationFactory.createUserController();
        infraFactory.setUserReplicationCallback(v -> userController.getUserListUpdateCallback().run());

        ServerNetworkAdapter singlePeerNetworkControl = infraFactory.createServerNetworkAdapter();

        serviceFactory.getReplicationNotifierProxy().setDelegate(singlePeerNetworkControl);

        serverControl.setPeerNetworkControl(singlePeerNetworkControl);
        presentationFactory.setServerNetworkControl(singlePeerNetworkControl);

        // Start HTTP REST API server (configurable port via HTTP_PORT env var, default 8085)
        int httpPort = Integer.parseInt(System.getenv().getOrDefault("HTTP_PORT", "8085"));
        HttpRestServer httpServer = presentationFactory.createHttpRestServer(httpPort);
        httpServer.start();

        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}