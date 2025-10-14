package com.clientPresentation.views;

import com.chatCommon.dto.UserDTO;
import com.clientApplication.factories.CommandFactory;
import com.clientApplication.ports.ServerGatewayPort;
import com.clientPresentation.views.actions.ConnectionPanel;
import com.clientPresentation.views.actions.LoginPanel;
import java.util.Base64;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MainClientView extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private CommandFactory commandFactory;
    private ServerGatewayPort gateway;
    private String loggedInUsername;
    private ChatPanel chatPanel;

    public MainClientView() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                if (gateway != null) {
                    System.out.println("Cerrando la ventana, desconectando del servidor...");
                    gateway.disconnect(); // Desconexión elegante
                }
                System.exit(0); // Ahora sí, cerrar la aplicación
            }
        });

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

    private void onConnectionSuccess(CommandFactory commandFactory, ServerGatewayPort gateway) {
        this.commandFactory = commandFactory;
        this.gateway = gateway;
        System.out.println("Conexión exitosa. Creando panel de login...");

        LoginPanel loginPanel = new LoginPanel(
                commandFactory.createLoginCommand(),
                this::onLoginSuccess // MODIFICAR: ahora el onLoginSuccess necesita el username
        );

        mainPanel.add(loginPanel, "LOGIN_PANEL");
        cardLayout.show(mainPanel, "LOGIN_PANEL");
    }

    private void onReturnToConnection() {
        System.out.println("Regresando al panel de conexión...");
        cardLayout.show(mainPanel, "CONNECTION_PANEL");
    }

    private void onLoginSuccess(String username) { // MODIFICAR: Recibir el username
        this.loggedInUsername = username; // Guardar
        setTitle("Chat Universitario - ¡Bienvenido, " + username + "!");

        // Guardamos la referencia al ChatPanel para poder llamarlo
        this.chatPanel = new ChatPanel(loggedInUsername, commandFactory); // MODIFICAR: pasar username y factory

        mainPanel.add(chatPanel, "CHAT_PANEL");
        cardLayout.show(mainPanel, "CHAT_PANEL");

        // CONFIGURAR EL LISTENER
        this.gateway.setAsyncMessageListener(parts -> {
            if (parts == null || parts.isEmpty()) return;

            String command = parts.getFirst();
            switch (command.toUpperCase()) {
                case "USER_CONNECTED":
                    if (parts.size() >= 3) {
                        UserDTO newUser = new UserDTO(parts.get(1), parts.get(2));
                        chatPanel.addUserToList(newUser);
                    }
                    break;
                case "USER_DISCONNECTED":
                    if (parts.size() >= 2) {
                        chatPanel.removeUserFromList(parts.get(1));
                    }
                    break;
                case "RECEIVE_PRIVATE_MESSAGE": // <-- NUEVO CASO
                    if (parts.size() >= 3) {
                        String sender = parts.get(1);
                        String content = parts.get(2);
                        // Asegurarnos de que el panel de chat exista antes de usarlo
                        if (this.chatPanel != null) {
                            this.chatPanel.receiveMessage(sender, content);
                        }
                    }
                    break;
                case "RECEIVE_PRIVATE_AUDIO": // <-- NUEVO CASO
                    if (parts.size() >= 3) {
                        String sender = parts.get(1);
                        String audioBase64 = parts.get(2);

                        // Decodificar de Base64 a byte[]
                        byte[] audioData = Base64.getDecoder().decode(audioBase64);

                        if (this.chatPanel != null) {
                            // Necesitamos un nuevo método en ChatPanel para esto
                            this.chatPanel.receiveAudioMessage(sender, audioData);
                        }
                    }
                    break;
            }
        });
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