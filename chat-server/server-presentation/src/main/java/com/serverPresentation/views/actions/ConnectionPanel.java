package com.serverPresentation.views.actions;

import com.chatCommon.viewResources.UiBuilder;
import com.serverInfrastructure.network.TcpServer;
import com.serverPresentation.factories.ServerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionPanel extends JPanel {

    private final ServerFactory factory;
    private JTextField portField;
    private JButton toggleServerButton;
    private JLabel statusLabel;
    private JLabel ipLabel;

    private TcpServer currentServer;
    private Thread serverThread;

    public ConnectionPanel(ServerFactory factory) {
        this.factory = factory;
        initComponents();
        loadServerIp();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createLineBorder(new Color(210, 210, 210))
        ));

        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createTitledBorder("Gestión del Servidor"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ipLabel = new JLabel("IP del Servidor: Cargando...");
        ipLabel.setFont(new Font("SansSerif", Font.BOLD, 14));

        Border roundedBorder = UiBuilder.createRoundedBorder();

        portField = new JTextField("12345", 10);
        UiBuilder.styleField(portField, roundedBorder);

        toggleServerButton = new JButton("Iniciar Servidor");
        statusLabel = new JLabel("Estado: Detenido");
        statusLabel.setForeground(Color.RED);

        UiBuilder.styleButton(toggleServerButton, new Color(46, 153, 85));

        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(ipLabel, gbc);

        gbc.gridy++; gbc.gridwidth = 1;
        add(new JLabel("Puerto:"), gbc);

        gbc.gridx = 1;
        add(portField, gbc);

        gbc.gridy++; gbc.gridx = 0; gbc.gridwidth = 2;
        add(toggleServerButton, gbc);

        gbc.gridy++;
        add(statusLabel, gbc);

        toggleServerButton.addActionListener(e -> onToggleServer());
    }

    private void onToggleServer() {
        if (currentServer == null) {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                currentServer = factory.createTcpServer(port);

                serverThread = new Thread(() -> {
                    try {
                        currentServer.start();
                    } catch (IOException e) {
                        SwingUtilities.invokeLater(() -> showError("Error al ejecutar el servidor: " + e.getMessage()));
                        resetServerState();
                    }
                });

                serverThread.start();
                updateUI(true, port);
            } catch (NumberFormatException ex) {
                showError("El puerto debe ser un número válido.");
            }
        } else {
            // Detener servidor
            currentServer.stop();
            serverThread.interrupt(); // Opcional, pero buena práctica
            resetServerState();
        }
    }

    private void resetServerState() {
        currentServer = null;
        serverThread = null;
        SwingUtilities.invokeLater(() -> updateUI(false, 0));
    }

    private void updateUI(boolean isRunning, int port) {
        if (isRunning) {
            statusLabel.setText("Estado: Escuchando en el puerto " + port);
            statusLabel.setForeground(new Color(46, 153, 85));
            toggleServerButton.setText("Detener Servidor");
            UiBuilder.styleButton(toggleServerButton, new Color(220, 38, 38));
            portField.setEnabled(false);
        } else {
            statusLabel.setText("Estado: Detenido");
            statusLabel.setForeground(Color.RED);
            toggleServerButton.setText("Iniciar Servidor");
            UiBuilder.styleButton(toggleServerButton, new Color(46, 153, 85));
            portField.setEnabled(true);
        }
    }

    private void loadServerIp() {
        try {
            ipLabel.setText("IP del Servidor: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            ipLabel.setText("IP del Servidor: No se pudo determinar.");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}