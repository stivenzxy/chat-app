package com.clientPresentation.views;

import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.views.actions.ConnectionPanel;
import com.clientPresentation.views.actions.LoginPanel;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainClientView extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private CommandFactory commandFactory;

    public MainClientView() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(230, 230, 230));

        try {
            String clientIp = InetAddress.getLocalHost().getHostAddress();
            setTitle("Chat Universitario - Cliente [" + clientIp + "]");
        } catch (UnknownHostException e) {
            setTitle("Chat Universitario - Cliente [IP desconocida]");
        }

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        ConnectionPanel connectionPanel = new ConnectionPanel(this::onConnectionSuccess, this::onReturnToConnection);

        mainPanel.add(connectionPanel, "CONNECTION_PANEL");

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(mainPanel, BorderLayout.CENTER);

        cardLayout.show(mainPanel, "CONNECTION_PANEL");
    }

    private void onConnectionSuccess(CommandFactory commandFactory) {
        this.commandFactory = commandFactory; // Guardar la fábrica
        System.out.println("Conexión exitosa. Creando panel de login...");

        // Pasamos un "callback" o una acción a ejecutar cuando el login sea exitoso
        LoginPanel loginPanel = new LoginPanel(
                commandFactory.createLoginCommand(),
                this::onLoginSuccess // Referencia al método
        );

        mainPanel.add(loginPanel, "LOGIN_PANEL");
        cardLayout.show(mainPanel, "LOGIN_PANEL");
    }
    
    private void onReturnToConnection() {
        System.out.println("Regresando al panel de conexión...");
        cardLayout.show(mainPanel, "CONNECTION_PANEL");
    }

    // Nuevo método que se llamará desde LoginPanel
    private void onLoginSuccess() {
        // Ocultar el subtítulo de "conéctate al servidor"
        // (Este es un poco más complejo, por ahora lo dejamos)
        setTitle("Chat Universitario - ¡Bienvenido!");

        ChatPanel chatPanel = new ChatPanel(commandFactory.createGetUsersCommand());
        mainPanel.add(chatPanel, "CHAT_PANEL");
        cardLayout.show(mainPanel, "CHAT_PANEL");
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 0, 5, 0);

        JLabel titleLabel = new JLabel("¡Bienvenido al Chat Universitario!");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setForeground(new Color(55, 65, 81));
        headerPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Primero, conéctate al servidor");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(107, 114, 128));
        gbc.insets = new Insets(5, 0, 20, 0);
        headerPanel.add(subtitleLabel, gbc);
        return headerPanel;
    }
}