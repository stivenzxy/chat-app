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
        
        // ======================= INICIO DE LA CORRECCIÓN =======================

        // 1. Crea UNA SOLA instancia del adaptador de red P2P.
        ServerNetworkAdapter singlePeerNetworkControl = infraFactory.createServerNetworkAdapter();

        // 2. Pasa esta ÚNICA instancia tanto al control del servidor principal...
        serverControl.setPeerNetworkControl(singlePeerNetworkControl);

        // 3. ...como a la factoría de presentación. La factoría ya no creará una nueva.
        //    (Necesitarás un pequeño ajuste en PresentationFactory para aceptar esto).
        PresentationFactory presentationFactory = new PresentationFactory(
            serviceFactory, 
            infraFactory, 
            serverControl,
            singlePeerNetworkControl // <-- Pasa la instancia única aquí
        );

        // ======================== FIN DE LA CORRECCIÓN ========================


        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}