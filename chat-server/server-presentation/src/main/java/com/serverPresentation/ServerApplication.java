package com.serverPresentation;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.adapters.TcpServerAdapter;
import com.serverInfrastructure.factories.DefaultServiceFactory;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.factories.PresentationFactory;
import com.serverPresentation.views.MainServerView;

import javax.swing.*;

public class ServerApplication {
    public static void main(String[] args) {
        ServiceFactory serviceFactory = new DefaultServiceFactory();
        InfrastructureFactory infraFactory = new InfrastructureFactory(serviceFactory);
        TcpServerAdapter serverControl = new TcpServerAdapter(infraFactory);

        // Create presentation factory first to get UserController
        PresentationFactory presentationFactory = new PresentationFactory(
            serviceFactory, 
            infraFactory, 
            serverControl,
            null  // Will set network control after connecting callback
        );
        
        // Connect UserController's observable to replication system
        UserController userController = presentationFactory.createUserController();
        infraFactory.setUserReplicationCallback(v -> userController.getUserListUpdateCallback().run());
        
        // Now create ServerNetworkAdapter with callback configured
        ServerNetworkAdapter singlePeerNetworkControl = infraFactory.createServerNetworkAdapter();
        
        // Wire up the replication notifier proxy to the actual implementation
        if (serviceFactory instanceof DefaultServiceFactory) {
            ((DefaultServiceFactory) serviceFactory).getReplicationNotifierProxy().setDelegate(singlePeerNetworkControl);
        }

        serverControl.setPeerNetworkControl(singlePeerNetworkControl);
        presentationFactory.setServerNetworkControl(singlePeerNetworkControl);

        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}