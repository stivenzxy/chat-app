package com.serverPresentation;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.adapters.TcpServerAdapter;
import com.serverInfrastructure.factories.DefaultServiceFactory;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverPresentation.factories.PresentationFactory;
import com.serverPresentation.views.MainServerView;

import javax.swing.*;

public class ServerApplication {
    public static void main(String[] args) {
        ServiceFactory serviceFactory = new DefaultServiceFactory();
        InfrastructureFactory infraFactory = new InfrastructureFactory(serviceFactory);
        TcpServerAdapter serverControl = new TcpServerAdapter(infraFactory);

        ServerNetworkAdapter singlePeerNetworkControl = infraFactory.createServerNetworkAdapter();

        serverControl.setPeerNetworkControl(singlePeerNetworkControl);

        PresentationFactory presentationFactory = new PresentationFactory(
            serviceFactory, 
            infraFactory, 
            serverControl,
            singlePeerNetworkControl
        );

        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}