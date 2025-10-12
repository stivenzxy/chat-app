package com.clientPresentation;

import com.clientPresentation.views.MainClientView;

import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            MainClientView mainView = new MainClientView();
            mainView.setVisible(true);
        });
    }
}
