package com.serverPresentation.factories;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
import com.serverApplication.ports.peer.PeerNetworkControl;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.MainServerView;
import com.serverPresentation.views.components.ConnectionPanel;
import com.serverPresentation.views.components.ServerNetworkPanel;
import com.serverPresentation.views.components.UserListPanel;


public class PresentationFactory {
    private final ServiceFactory serviceFactory;
    private final InfrastructureFactory infrastructureFactory;
    private final ServerControl serverControl;
    private final PeerNetworkControl serverNetworkControl; // <-- Ya tienes el campo, solo cambiaremos cómo se inicializa

    private UserController userController;

    // ============== CONSTRUCTOR MODIFICADO ==============
    public PresentationFactory(
            ServiceFactory serviceFactory, 
            InfrastructureFactory infrastructureFactory, 
            ServerControl serverControl,
            PeerNetworkControl serverNetworkControl // <-- AÑADIR este parámetro
    ) {
        this.serviceFactory = serviceFactory;
        this.infrastructureFactory = infrastructureFactory;
        this.serverControl = serverControl;
        
        // Asigna la instancia única que recibes, en lugar de crear una nueva.
        this.serverNetworkControl = serverNetworkControl; 
    }

    public UserController createUserController() {
        if (userController == null) {
            userController = new UserController(
                serviceFactory.createUserService(), 
                serviceFactory.createGetUsersPresentationService()
            );
        }
        return userController;
    }

    public UserListPanel createUserListPanel() {
        return new UserListPanel(createUserController());
    }

    public ConnectionPanel createConnectionPanel() {
        return new ConnectionPanel(serverControl, this);
    }
    
    public ServerNetworkPanel createServerNetworkPanel() {
        return new ServerNetworkPanel(serverNetworkControl);
    }

    public MainServerView createMainServerView() {
        return new MainServerView(this);
    }
    
    public ServerControl getServerControl() {
        return serverControl;
    }
    
    public PeerNetworkControl getServerNetworkControl() {
        return serverNetworkControl;
    }
}