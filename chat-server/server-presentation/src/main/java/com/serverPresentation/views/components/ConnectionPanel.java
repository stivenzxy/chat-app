package com.serverPresentation.views.components;

import com.chatCommon.viewResources.UiBuilder;
import com.serverApplication.dto.ConnectedClientInfo;
import com.serverApplication.ports.ServerControl;
import com.serverApplication.ports.ClientConnectionObserver;
import com.serverPresentation.factories.PresentationFactory;
import com.serverPresentation.views.models.ConnectionTableModel;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.observers.ActiveUserObserver;
import com.serverDomain.entities.User;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public class ConnectionPanel extends JPanel implements ClientConnectionObserver, ActiveUserObserver {
    private JButton toggleServerButton;
    private JLabel statusLabel;
    private JLabel ipLabel;
    private JLabel portLabel;
    private JLabel connectionPoolLabel;
    private JTable connectionsTable;
    private ConnectionTableModel tableModel;
    private BroadcastPanel broadcastPanel;
    private ServerNetworkPanel serverNetworkPanel;

    private final ServerControl serverControl;
    private final PresentationFactory factory;

    public ConnectionPanel(ServerControl serverControl, PresentationFactory factory) {
        this.serverControl = serverControl;
        this.factory = factory;
        this.serverControl.addConnectionObserver(this);
        
        ActiveUserManager.getInstance().addObserver(this);
        
        initComponents();
        loadServerIp();
        loadServerPort();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(createControlPanel(), BorderLayout.NORTH);
        add(createTabbedPane(), BorderLayout.CENTER);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Control del Servidor"));

        ipLabel = new JLabel("IP del Servidor: Cargando...");
        ipLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(ipLabel);

        portLabel = new JLabel("Puerto del Servidor: Cargando...");
        portLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(portLabel);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(2, 20));
        panel.add(separator);

        toggleServerButton = new JButton("Iniciar Servidor");
        UiBuilder.styleButton(toggleServerButton, new Color(46, 153, 85));
        toggleServerButton.addActionListener(e -> onToggleServer());
        panel.add(toggleServerButton);

        statusLabel = new JLabel("Estado: Detenido");
        statusLabel.setForeground(Color.RED);
        panel.add(statusLabel);

        return panel;
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel connectionsTab = createConnectionsTablePanel();
        tabbedPane.addTab("Conexiones", connectionsTab);

        broadcastPanel = new BroadcastPanel(serverControl);
        tabbedPane.addTab("Broadcast", broadcastPanel);
        
        serverNetworkPanel = factory.createServerNetworkPanel();
        tabbedPane.addTab("Red de Servidores", serverNetworkPanel);
        
        return tabbedPane;
    }

    private JPanel createConnectionsTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Conexiones Entrantes"));

        connectionPoolLabel = new JLabel("Pool de conexiones: Servidor detenido");
        panel.add(connectionPoolLabel, BorderLayout.NORTH);

        tableModel = new ConnectionTableModel();
        connectionsTable = new JTable(tableModel);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(new JScrollPane(connectionsTable), BorderLayout.CENTER);

        JPanel buttonPanel = createConnectionButtonsPanel();
        tablePanel.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(tablePanel, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createConnectionButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton disconnectButton = new JButton("Desconectar Seleccionados");
        UiBuilder.styleButton(disconnectButton, new Color(220, 38, 38));
        disconnectButton.addActionListener(e -> onDisconnectSelected());
        
        JButton selectAllButton = new JButton("Seleccionar Todo");
        UiBuilder.styleButton(selectAllButton, new Color(59, 130, 246));
        selectAllButton.addActionListener(e -> onSelectAll());
        
        JButton clearSelectionButton = new JButton("Limpiar Selección");
        UiBuilder.styleButton(clearSelectionButton, new Color(107, 114, 128));
        clearSelectionButton.addActionListener(e -> onClearSelection());
        
        panel.add(selectAllButton);
        panel.add(clearSelectionButton);
        panel.add(disconnectButton);
        
        return panel;
    }

    private void onToggleServer() {
        boolean isServerRunning = toggleServerButton.getText().equals("Detener Servidor");

        if (!isServerRunning) {
            try {
                serverControl.startServer();
                int port = serverControl.getServerPortNumber();
                updateUI(true, port);

                if (serverNetworkPanel != null) {
                    serverNetworkPanel.updatePeerServerStatus(true);
                }
            } catch (NumberFormatException ex) {
                showError("El puerto debe ser un número válido.");
            } catch (Exception ex) {
                showError("No se pudo iniciar el servidor: " + ex.getMessage());
            }
        } else {
            serverControl.stopServer();
            updateUI(false, 0);

            if (serverNetworkPanel != null) {
                serverNetworkPanel.updatePeerServerStatus(false);
            }
        }
    }

    @Override
    public void onClientConnected(ConnectedClientInfo clientInfo) {
        SwingUtilities.invokeLater(() -> {
            tableModel.addConnection(clientInfo);
            updateConnectionPoolLabel();
        });
    }

    @Override
    public void onClientDisconnected(ConnectedClientInfo clientInfo) {
        SwingUtilities.invokeLater(() -> {
            tableModel.removeConnection(clientInfo);
            updateConnectionPoolLabel();
        });
    }

    private void updateConnectionPoolLabel() {
        int current = serverControl.getCurrentConnections();
        int max = serverControl.getMaxConnections();
        connectionPoolLabel.setText(String.format("Pool de conexiones: %d/%d", current, max));
    }
    
    private void onDisconnectSelected() {
        List<ConnectedClientInfo> selectedClients = tableModel.getSelectedConnections();
        if (selectedClients.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No hay clientes seleccionados para desconectar.", 
                "Información", 
                JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        int confirmed = JOptionPane.showConfirmDialog(this, 
            String.format("¿Está seguro de desconectar %d cliente(s) seleccionado(s)?", selectedClients.size()), 
            "Confirmar Desconexión", 
            JOptionPane.YES_NO_OPTION, 
            JOptionPane.WARNING_MESSAGE);
            
        if (confirmed == JOptionPane.YES_OPTION) {
            for (ConnectedClientInfo client : selectedClients) {
                serverControl.disconnectClient(client.connectionId());
            }
            tableModel.clearSelections();
        }
    }
    
    private void onSelectAll() {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            tableModel.setValueAt(true, i, 0);
        }
    }
    
    private void onClearSelection() {
        tableModel.clearSelections();
    }

    private void updateUI(boolean isRunning, int port) {
        if (isRunning) {
            statusLabel.setText("Estado: Escuchando en el puerto " + port);
            statusLabel.setForeground(new Color(46, 153, 85));
            toggleServerButton.setText("Detener Servidor");
            UiBuilder.styleButton(toggleServerButton, new Color(220, 38, 38));
            updateConnectionPoolLabel();
        } else {
            statusLabel.setText("Estado: Detenido");
            statusLabel.setForeground(Color.RED);
            toggleServerButton.setText("Iniciar Servidor");
            UiBuilder.styleButton(toggleServerButton, new Color(46, 153, 85));
            connectionPoolLabel.setText("Pool de conexiones: Servidor detenido");
        }

        if (broadcastPanel != null) {
            broadcastPanel.setServerRunning(isRunning);
        }
    }

    private void loadServerIp() {
        try {
            ipLabel.setText("IP del Servidor: " + InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            ipLabel.setText("IP del Servidor: No se pudo determinar.");
        }
    }

    private void loadServerPort() {
        try {
            int port = serverControl.getServerPortNumber();
            if (port > 0) {
                portLabel.setText("Puerto del Servidor: " + port);
            } else {
                portLabel.setText("Puerto del Servidor: No configurado");
            }
        } catch (Exception e) {
            portLabel.setText("Puerto del Servidor: Error al cargar" + e.getMessage());
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    @Override
    public void onUserLoggedIn(User user) {
        SwingUtilities.invokeLater(() -> {
            tableModel.updateConnectionUsername(user.getId(), user.getUsername().value());
        });
    }
    
    @Override
    public void onUserLoggedOut(User user, boolean isLastSession) {
        SwingUtilities.invokeLater(() -> {
            tableModel.updateConnectionUsername(user.getId(), null);
        });
    }
}