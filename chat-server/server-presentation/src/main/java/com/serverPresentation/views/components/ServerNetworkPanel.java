package com.serverPresentation.views.components;

import com.chatCommon.viewResources.UiBuilder;
import com.serverPresentation.views.models.ServerNetworkTableModel;
import com.serverPresentation.views.models.ServerNetworkTableModel.ConnectedServerInfo;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.LocalDateTime;

public class ServerNetworkPanel extends JPanel {
    
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectSelectedButton;
    private JTable serversTable;
    private ServerNetworkTableModel tableModel;
    private JLabel statusLabel;
    private JLabel connectedServersLabel;
    
    public ServerNetworkPanel() {
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        add(createConnectionFormPanel(), BorderLayout.NORTH);
        add(createServersTablePanel(), BorderLayout.CENTER);
    }
    
    private JPanel createConnectionFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Conectar a Otro Servidor"));
        
        // Panel de descripción
        JPanel descriptionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel descLabel = new JLabel("<html><i>Conecta este servidor a otros servidores en la red P2P para compartir usuarios y canales</i></html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        descLabel.setForeground(new Color(75, 85, 99));
        descriptionPanel.add(descLabel);
        panel.add(descriptionPanel, BorderLayout.NORTH);
        
        // Panel de campos de entrada
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        Border roundedBorder = UiBuilder.createRoundedBorder();
        
        inputPanel.add(new JLabel("IP del Servidor:"));
        ipField = new JTextField("127.0.0.1", 15);
        UiBuilder.styleField(ipField, roundedBorder);
        inputPanel.add(ipField);
        
        inputPanel.add(new JLabel("Puerto:"));
        portField = new JTextField("12346", 8);
        UiBuilder.styleField(portField, roundedBorder);
        inputPanel.add(portField);
        
        connectButton = new JButton("Conectar");
        UiBuilder.styleButton(connectButton, new Color(59, 130, 246));
        connectButton.setPreferredSize(new Dimension(120, 35));
        connectButton.addActionListener(e -> onConnectToServer());
        inputPanel.add(connectButton);
        
        panel.add(inputPanel, BorderLayout.CENTER);
        
        // Panel de estado
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Estado: Listo para conectar");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusPanel.add(statusLabel);
        panel.add(statusPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createServersTablePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Servidores Conectados"));
        
        // Label de contador
        connectedServersLabel = new JLabel("Servidores en la red: 0");
        connectedServersLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        panel.add(connectedServersLabel, BorderLayout.NORTH);
        
        // Tabla de servidores
        tableModel = new ServerNetworkTableModel();
        serversTable = new JTable(tableModel);
        serversTable.setRowHeight(25);
        serversTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        serversTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        serversTable.getColumnModel().getColumn(0).setMaxWidth(100);
        
        JScrollPane scrollPane = new JScrollPane(serversTable);
        scrollPane.setPreferredSize(new Dimension(0, 250));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = createTableButtonsPanel();
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createTableButtonsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        
        JButton selectAllButton = new JButton("Seleccionar Todo");
        UiBuilder.styleButton(selectAllButton, new Color(59, 130, 246));
        selectAllButton.addActionListener(e -> onSelectAll());
        
        JButton clearSelectionButton = new JButton("Limpiar Selección");
        UiBuilder.styleButton(clearSelectionButton, new Color(107, 114, 128));
        clearSelectionButton.addActionListener(e -> onClearSelection());
        
        disconnectSelectedButton = new JButton("Desconectar Seleccionados");
        UiBuilder.styleButton(disconnectSelectedButton, new Color(220, 38, 38));
        disconnectSelectedButton.addActionListener(e -> onDisconnectSelected());
        
        JButton refreshButton = new JButton("Refrescar Estado");
        UiBuilder.styleButton(refreshButton, new Color(16, 185, 129));
        refreshButton.addActionListener(e -> onRefreshServers());
        
        panel.add(selectAllButton);
        panel.add(clearSelectionButton);
        panel.add(disconnectSelectedButton);
        panel.add(refreshButton);
        
        return panel;
    }
    
    private void onConnectToServer() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        
        if (ip.isEmpty()) {
            showError("Por favor ingrese la IP del servidor.");
            return;
        }
        
        if (portStr.isEmpty()) {
            showError("Por favor ingrese el puerto del servidor.");
            return;
        }
        
        try {
            int port = Integer.parseInt(portStr);
            
            if (port < 1 || port > 65535) {
                showError("El puerto debe estar entre 1 y 65535.");
                return;
            }
            
            // Validar que no esté ya conectado
            if (isServerAlreadyConnected(ip, port)) {
                showWarning("Ya existe una conexión con este servidor.");
                return;
            }
            
            // TODO: Aquí irá la lógica real de conexión P2P
            // Por ahora solo agregamos a la tabla como simulación
            ConnectedServerInfo serverInfo = new ConnectedServerInfo(
                ip, 
                port, 
                "Conectado", 
                LocalDateTime.now()
            );
            
            tableModel.addServer(serverInfo);
            updateConnectedServersLabel();
            
            statusLabel.setText("Estado: Conectado a " + ip + ":" + port);
            statusLabel.setForeground(new Color(16, 185, 129));
            
            showInfo("Conexión establecida exitosamente con " + ip + ":" + port);
            
            ipField.setText("");
            portField.setText("");
            
        } catch (NumberFormatException e) {
            showError("El puerto debe ser un número válido.");
        }
    }
    
    private void onDisconnectSelected() {
        var selectedServers = tableModel.getSelectedServers();
        
        if (selectedServers.isEmpty()) {
            showInfo("No hay servidores seleccionados para desconectar.");
            return;
        }
        
        int confirmed = JOptionPane.showConfirmDialog(
            this,
            String.format("¿Está seguro de desconectar %d servidor(es) seleccionado(s)?", selectedServers.size()),
            "Confirmar Desconexión",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        
        if (confirmed == JOptionPane.YES_OPTION) {
            for (ConnectedServerInfo server : selectedServers) {
                tableModel.removeServer(server.getIpAddress(), server.getPort());
            }
            
            updateConnectedServersLabel();
            statusLabel.setText("Estado: Servidores desconectados");
            statusLabel.setForeground(new Color(220, 38, 38));
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
    
    private void onRefreshServers() {
        statusLabel.setText("Estado: Verificando conexiones...");
        statusLabel.setForeground(new Color(251, 191, 36));

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Estado: Servidores actualizados");
            statusLabel.setForeground(new Color(16, 185, 129));
        });
    }
    
    private boolean isServerAlreadyConnected(String ip, int port) {
        for (ConnectedServerInfo server : tableModel.getAllServers()) {
            if (server.getIpAddress().equals(ip) && server.getPort() == port) {
                return true;
            }
        }
        return false;
    }
    
    private void updateConnectedServersLabel() {
        int count = tableModel.getRowCount();
        connectedServersLabel.setText("Servidores en la red: " + count);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(this, message, "Advertencia", JOptionPane.WARNING_MESSAGE);
    }
    
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(this, message, "Información", JOptionPane.INFORMATION_MESSAGE);
    }
}
