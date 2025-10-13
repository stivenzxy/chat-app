package com.serverPresentation.views;

import com.serverPresentation.controllers.UserController;
import com.serverPresentation.factories.ServerFactory;
import com.serverPresentation.views.actions.ConnectionPanel;
import com.serverPresentation.views.actions.RegisterUserPanel;

import javax.swing.*;
import java.awt.*;

public class MainServerView extends JFrame {

    private final UserController userController;
    private final ServerFactory factory;

    private JPanel cardsPanel;
    private CardLayout cardLayout;

    public MainServerView(UserController userController, ServerFactory factory) {
        this.userController = userController;
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {
        setTitle("Chat universitario - Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        JMenuBar menuBar = new JMenuBar();
        JMenu navigationMenu = new JMenu("Navegación");
        JMenuItem userManagementItem = new JMenuItem("Gestión de Usuarios");
        JMenuItem connectionManagementItem = new JMenuItem("Gestión de Conexiones");

        navigationMenu.add(userManagementItem);
        navigationMenu.add(connectionManagementItem);
        menuBar.add(navigationMenu);
        setJMenuBar(menuBar);

        userManagementItem.addActionListener(e -> cardLayout.show(cardsPanel, "USER_MANAGEMENT"));
        connectionManagementItem.addActionListener(e -> cardLayout.show(cardsPanel, "CONNECTION_MANAGEMENT"));

        JPanel userManagementPanel = createUserManagementPanel();
        JPanel connectionManagementPanel = createConnectionManagementPanel();

        cardsPanel.add(userManagementPanel, "USER_MANAGEMENT");
        cardsPanel.add(connectionManagementPanel, "CONNECTION_MANAGEMENT");

        add(cardsPanel, BorderLayout.CENTER);

        cardLayout.show(cardsPanel, "CONNECTION_MANAGEMENT");
    }

    private JPanel createUserManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 230, 230));

        JLabel titleLabel = new JLabel("Gestión de Usuarios");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTabbedPane userTabs = new JTabbedPane();
        userTabs.addTab("Registrar Usuario", new RegisterUserPanel(userController));
        userTabs.addTab("Listado de Usuarios", new JPanel());
        panel.add(userTabs, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createConnectionManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(230, 230, 230));

        JLabel titleLabel = new JLabel("Gestión de Conexiones");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        panel.add(titleLabel, BorderLayout.NORTH);

        ConnectionPanel connectionPanel = new ConnectionPanel(factory);
        connectionPanel.setOpaque(true);
        connectionPanel.setBackground(Color.WHITE);
        connectionPanel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(
                                BorderFactory.createLineBorder(new Color(210, 210, 210), 1, true),
                                "Servidor TCP"
                        ),
                        BorderFactory.createEmptyBorder(20, 20, 20, 20)
                )
        );

        panel.add(connectionPanel, BorderLayout.CENTER);
        return panel;
    }

}