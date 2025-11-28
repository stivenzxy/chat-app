package com.serverPresentation;

import com.chatCommon.utils.AppProperties;
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

        // Leer el puerto HTTP desde el archivo de configuración
        AppProperties props = new AppProperties("server-configuration");
        int httpPort = props.getInt("HTTP_PORT");
        HttpRestServer httpServer = presentationFactory.createHttpRestServer(httpPort);
        
        // Configurar el TcpServerAdapter para métricas de conexiones TCP
        httpServer.setTcpServerAdapter(serverControl);
        
        httpServer.start();

        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}