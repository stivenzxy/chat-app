package com.clientPresentation.views;

import com.chatCommon.dto.UserDTO;
import com.clientApplication.events.*;
import com.clientApplication.factories.CommandFactory;
import com.clientApplication.factories.MessageHandlerFactory;
import com.clientApplication.handlers.MessageHandler;
import com.clientApplication.listeners.*;
import com.clientApplication.ports.ServerGatewayPort;
import com.clientPresentation.views.actions.ConnectionPanel;
import com.clientPresentation.views.actions.LoginPanel;

import javax.swing.*;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;

public class MainClientView extends JFrame implements 
        UserConnectionListener, 
        UserDisconnectionListener, 
        PrivateMessageListener, 
        PrivateAudioListener {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private CommandFactory commandFactory;
    private ServerGatewayPort gateway;
    private String loggedInUsername;
    private ChatPanel chatPanel;
    private MessageHandler messageHandler;

    public MainClientView() {
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                handleDisconnectAndExit();
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

    private void handleDisconnectAndExit() {
        if (gateway != null) {
            System.out.println("Cerrando la ventana, desconectando del servidor...");
            gateway.disconnect();
        }
        System.exit(0);
    }

    public void handleDisconnectAndReturnToConnection() {
        if (gateway != null) {
            System.out.println("Desconectando del servidor...");
            gateway.disconnect();
        }

        this.gateway = null;
        this.commandFactory = null;
        this.loggedInUsername = null;
        this.chatPanel = null;
        this.messageHandler = null;

        mainPanel.removeAll();

        ConnectionPanel newConnectionPanel = new ConnectionPanel(this::onConnectionSuccess, this::onReturnToConnection);
        mainPanel.add(newConnectionPanel, "CONNECTION_PANEL");

        cardLayout.show(mainPanel, "CONNECTION_PANEL");
        setTitle("Chat Universitario - Cliente [Desconectado]");

        revalidate();
        repaint();
    }

    private void onConnectionSuccess(CommandFactory commandFactory, ServerGatewayPort gateway) {
        this.commandFactory = commandFactory;
        this.gateway = gateway;
        System.out.println("Conexión exitosa. Creando panel de login...");

        LoginPanel loginPanel = new LoginPanel(
                commandFactory.createLoginCommand(),
                this::onLoginSuccess,
                this::handleDisconnectAndReturnToConnection
        );

        mainPanel.add(loginPanel, "LOGIN_PANEL");
        cardLayout.show(mainPanel, "LOGIN_PANEL");
    }

    private void onReturnToConnection() {
        System.out.println("Regresando al panel de conexión...");
        cardLayout.show(mainPanel, "CONNECTION_PANEL");
    }

    private void onLoginSuccess(String username) {
        this.loggedInUsername = username;
        setTitle("Chat Universitario - ¡Bienvenido, " + username + "!");

        this.chatPanel = new ChatPanel(loggedInUsername, commandFactory, this::handleDisconnectAndReturnToConnection);
        mainPanel.add(chatPanel, "CHAT_PANEL");
        cardLayout.show(mainPanel, "CHAT_PANEL");

        this.messageHandler = MessageHandlerFactory.createAsyncMessageHandler();

        messageHandler.registerUserConnectionListener(this);
        messageHandler.registerUserDisconnectionListener(this);
        messageHandler.registerPrivateMessageListener(this);
        messageHandler.registerPrivateAudioListener(this);

        this.gateway.setAsyncMessageListener(parts -> {
            messageHandler.handleAsyncMessage(parts);
        });
    }

    @Override
    public void onUserConnected(UserConnectionEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (chatPanel != null) {
                UserDTO newUser = new UserDTO(event.userId(), event.username());
                chatPanel.addUserToList(newUser);
            }
        });
    }

    @Override
    public void onUserDisconnected(UserDisconnectionEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (chatPanel != null) {
                chatPanel.removeUserFromList(event.userId());
            }
        });
    }

    @Override
    public void onPrivateMessageReceived(PrivateMessageEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (chatPanel != null) {
                chatPanel.receiveMessage(event.sender(), event.content());
            }
        });
    }

    @Override
    public void onPrivateAudioReceived(PrivateAudioEvent event) {
        SwingUtilities.invokeLater(() -> {
            if (chatPanel != null) {
                try {
                    byte[] audioData = Base64.getDecoder().decode(event.audioBase64());
                    chatPanel.receiveAudioMessage(event.sender(), audioData);
                } catch (IllegalArgumentException e) {
                    System.err.println("Error decodificando audio: " + e.getMessage());
                }
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