package com.serverPresentation.factories;

import com.serverApplication.factories.ServiceFactory;
import com.serverApplication.ports.ServerControl;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.MainServerView;
import com.serverPresentation.views.components.ConnectionPanel;
import com.serverPresentation.views.components.UserListPanel;

public class PresentationFactory {
    private final ServiceFactory serviceFactory;
    private final ServerControl serverControl;

    private UserController userController;

    public PresentationFactory(ServiceFactory serviceFactory, ServerControl serverControl) {
        this.serviceFactory = serviceFactory;
        this.serverControl = serverControl;
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
        return new ConnectionPanel(serverControl);
    }

    public MainServerView createMainServerView() {
        return new MainServerView(this);
    }
}