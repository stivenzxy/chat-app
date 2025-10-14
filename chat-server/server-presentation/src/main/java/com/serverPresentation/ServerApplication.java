package com.serverPresentation;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
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
        ServerControl serverControl = new TcpServerAdapter(infraFactory);

        PresentationFactory presentationFactory = new PresentationFactory(serviceFactory, serverControl);

        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}