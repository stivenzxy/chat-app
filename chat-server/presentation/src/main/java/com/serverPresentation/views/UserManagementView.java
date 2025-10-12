package com.serverPresentation.views;

import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.actions.RegisterUserPanelAction;

import javax.swing.*;
import java.awt.*;

public class UserManagementView extends JFrame {

    private final UserController userController;

    public UserManagementView(UserController userController) {
        this.userController = userController;
        initComponents();
    }

    private void initComponents() {
        setTitle("Chat universitario - Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Opciones >");
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(230, 230, 230));

        JLabel titleLabel = new JLabel("Gestión de Usuarios");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Registrar", new RegisterUserPanelAction(userController));
        tabbedPane.addTab("Listado de usuarios", new JPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }
}