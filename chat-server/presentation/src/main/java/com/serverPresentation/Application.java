package com.serverPresentation;

import com.serverPresentation.factory.ServerFactory;
import com.serverPresentation.view.UserManagementView;

import javax.swing.*;

public class Application {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerFactory factory = new ServerFactory();
            UserManagementView view = factory.createUserManagementView();
            view.setVisible(true);
        });
    }
}
