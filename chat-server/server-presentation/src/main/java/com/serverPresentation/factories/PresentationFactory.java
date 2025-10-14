package com.serverPresentation.factories;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.MainServerView;
import com.serverPresentation.views.actions.ConnectionPanel;

public class PresentationFactory {
    private final ServiceFactory serviceFactory;
    private final ServerControl serverControl;

    public PresentationFactory(ServiceFactory serviceFactory, ServerControl serverControl) {
        this.serviceFactory = serviceFactory;
        this.serverControl = serverControl;
    }

    public UserController createUserController() {
        return new UserController(serviceFactory.createUserService());
    }

    public ConnectionPanel createConnectionPanel() {
        return new ConnectionPanel(serverControl);
    }

    public MainServerView createMainServerView() {
        return new MainServerView(this);
    }
}