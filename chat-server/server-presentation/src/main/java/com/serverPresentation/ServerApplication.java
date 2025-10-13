package com.serverPresentation;

import com.serverPresentation.factories.ServerFactory;
import com.serverPresentation.views.MainServerView;

import javax.swing.*;

public class ServerApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ServerFactory factory = new ServerFactory();
            MainServerView view = factory.createMainServerView();
            view.setVisible(true);
        });
    }
}