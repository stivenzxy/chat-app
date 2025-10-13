package com.serverPresentation.views.actions;

import com.chatCommon.viewResources.UiBuilder;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.ConnectionObserver;
import com.serverInfrastructure.network.TcpServer;
import com.serverPresentation.factories.ServerFactory;
import com.serverPresentation.views.models.ConnectionTableModel;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class ConnectionPanel extends JPanel implements ConnectionObserver {

    private final ServerFactory factory;
    private JTextField portField;
    private JButton toggleServerButton;
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JTable connectionsTable;
    private ConnectionTableModel tableModel;

    private TcpServer currentServer;
    private Thread serverThread;

    public ConnectionPanel(ServerFactory factory) {
        this.factory = factory;
        initComponents();
        loadServerIp();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createControlPanel(), BorderLayout.NORTH);
        add(createConnectionsTablePanel(), BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Control del Servidor"));

        Border roundedBorder = UiBuilder.createRoundedBorder();

        ipLabel = new JLabel("IP del Servidor: Cargando...");
        ipLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(ipLabel);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 20));
        panel.add(separator);

        panel.add(new JLabel("Puerto:"));
        portField = new JTextField("12345", 8);
        UiBuilder.styleField(portField, roundedBorder);
        panel.add(portField);

        toggleServerButton = new JButton("Iniciar Servidor");
        UiBuilder.styleButton(toggleServerButton, new Color(46, 153, 85));
        toggleServerButton.addActionListener(e -> onToggleServer());
        panel.add(toggleServerButton);

        statusLabel = new JLabel("Estado: Detenido");
        statusLabel.setForeground(Color.RED);
        panel.add(statusLabel);

        return panel;
    }

    private JPanel createConnectionsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Conexiones Entrantes"));

        panel.add(new JLabel("Máximo de conexiones: 10 (Pool)"), BorderLayout.NORTH);

        tableModel = new ConnectionTableModel();
        connectionsTable = new JTable(tableModel);

        panel.add(new JScrollPane(connectionsTable), BorderLayout.CENTER);
        return panel;
    }

    private void onToggleServer() {
        if (currentServer == null) {
            try {
                int port = Integer.parseInt(portField.getText().trim());
                currentServer = factory.createTcpServer(port);
                currentServer.addConnectionObserver(this);

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

    @Override
    public void onClientConnected(ClientConnection connection) {
        SwingUtilities.invokeLater(() -> tableModel.addConnection(connection));
    }

    @Override
    public void onClientDisconnected(ClientConnection connection) {
        SwingUtilities.invokeLater(() -> tableModel.removeConnection(connection));
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