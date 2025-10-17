package com.serverPresentation.views;

import com.serverPresentation.factories.PresentationFactory;
import com.serverPresentation.views.components.ConnectionPanel;
import com.serverPresentation.views.components.RegisterUserPanel;
import com.serverPresentation.views.components.ReportsPanel;

import javax.swing.*;
import java.awt.*;

public class MainServerView extends JFrame {

    private final PresentationFactory factory;

    private JPanel cardsPanel;
    private CardLayout cardLayout;

    public MainServerView(PresentationFactory factory) {
        this.factory = factory;
        initComponents();
    }

    private void initComponents() {
        setTitle("Chat universitario - Servidor");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(950, 600);
        setLocationRelativeTo(null);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                handleServerShutdown();
            }
        });

        cardLayout = new CardLayout();
        cardsPanel = new JPanel(cardLayout);

        JMenuBar menuBar = new JMenuBar();
        JMenu navigationMenu = new JMenu("Menú >");
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

        userTabs.addTab("Registrar Usuario", new RegisterUserPanel(factory.createUserController()));
        userTabs.addTab("Listado de Usuarios", factory.createUserListPanel());
        userTabs.addTab("Generar Informes", new ReportsPanel(factory));
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

        ConnectionPanel connectionPanel = factory.createConnectionPanel();
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
    
    private void handleServerShutdown() {
        int option = JOptionPane.showConfirmDialog(
            this,
            "¿Está seguro de que desea cerrar el servidor?\nTodos los clientes conectados serán desconectados.",
            "Confirmar Cierre del Servidor",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (option == JOptionPane.YES_OPTION) {
            factory.getServerControl().stopServer();

            System.exit(0);
        }
    }
}