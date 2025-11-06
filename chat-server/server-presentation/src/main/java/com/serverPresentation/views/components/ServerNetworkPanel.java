package com.serverPresentation.views.components;

import com.chatCommon.viewResources.UiBuilder;
import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverApplication.ports.PeerConnectionObserver;
import com.serverApplication.ports.peer.PeerNetworkControl;
import com.serverPresentation.views.models.ServerNetworkTableModel;
import com.serverPresentation.views.models.ServerNetworkTableModel.ConnectedServerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.List;

public class ServerNetworkPanel extends JPanel implements PeerConnectionObserver {
    private static final Logger logger = LoggerFactory.getLogger(ServerNetworkPanel.class);
    
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;
    private JButton disconnectSelectedButton;
    private JTable serversTable;
    private ServerNetworkTableModel tableModel;
    private JLabel statusLabel;
    private JLabel connectedServersLabel;
    private JLabel peerServerStatusLabel;
    private JLabel peerPortLabel;
    
    private final PeerNetworkControl serverNetworkControl;
    
    public ServerNetworkPanel(PeerNetworkControl serverNetworkControl) {
        this.serverNetworkControl = serverNetworkControl;
        this.serverNetworkControl.addPeerConnectionObserver(this);
        initComponents();
    }
    
    private JPanel createPeerServerControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Servidor P2P"));
        
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        
        // Puerto P2P (solo lectura desde configuración)
        peerPortLabel = new JLabel("Puerto P2P: " + serverNetworkControl.getPeerServerPort());
        peerPortLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        mainPanel.add(peerPortLabel);
        
        mainPanel.add(Box.createHorizontalStrut(10));
        
        // Estado P2P
        peerServerStatusLabel = new JLabel("Estado P2P: Esperando servidor principal");
        peerServerStatusLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        peerServerStatusLabel.setForeground(new Color(107, 114, 128));
        mainPanel.add(peerServerStatusLabel);
        
        panel.add(mainPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel superior: Control P2P y Conexión en la misma fila
        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.add(createPeerServerControlPanel(), BorderLayout.NORTH);
        topPanel.add(createConnectionFormPanel(), BorderLayout.CENTER);
        
        add(topPanel, BorderLayout.NORTH);
        add(createServersTablePanel(), BorderLayout.CENTER);
    }
    
    private JPanel createConnectionFormPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Conectar a Otro Servidor"));
        
        // Panel principal más compacto
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Border roundedBorder = UiBuilder.createRoundedBorder();
        
        mainPanel.add(new JLabel("IP:"));
        ipField = new JTextField("127.0.0.1", 12);
        UiBuilder.styleField(ipField, roundedBorder);
        mainPanel.add(ipField);
        
        mainPanel.add(new JLabel("Puerto:"));
        portField = new JTextField("12346", 6);
        UiBuilder.styleField(portField, roundedBorder);
        mainPanel.add(portField);
        
        connectButton = new JButton("Conectar");
        UiBuilder.styleButton(connectButton, new Color(59, 130, 246));
        connectButton.setPreferredSize(new Dimension(100, 30));
        connectButton.addActionListener(e -> onConnectToServer());
        mainPanel.add(connectButton);
        
        // Panel de estado más pequeño
        statusLabel = new JLabel("Estado: Listo para conectar");
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        mainPanel.add(Box.createHorizontalStrut(10)); // Espaciador
        mainPanel.add(statusLabel);
        
        panel.add(mainPanel, BorderLayout.CENTER);
        
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
        scrollPane.setPreferredSize(new Dimension(0, 180));
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
            String peerId = ip + ":" + port;
            if (serverNetworkControl.isConnectedToPeer(peerId)) {
                showWarning("Ya existe una conexión con este servidor.");
                return;
            }
            
            // Conectar usando el backend real
            statusLabel.setText("Estado: Conectando a " + peerId + "...");
            statusLabel.setForeground(new Color(251, 191, 36));
            
            boolean success = serverNetworkControl.connectToPeer(ip, port);
            
            if (success) {
                // NO mostrar alert aquí - esperar confirmación del callback onPeerConnected
                // El callback se encargará de notificar el éxito real
                
                // Limpiar campos
                ipField.setText("");
                portField.setText("");
                
            } else {
                statusLabel.setText("Estado: Error conectando a " + peerId);
                statusLabel.setForeground(new Color(220, 38, 38));
                showError("No se pudo establecer conexión con " + peerId);
            }
            
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
            // Convertir a peerIds para el backend
            List<String> peerIds = selectedServers.stream()
                .map(server -> server.getIpAddress() + ":" + server.getPort())
                .toList();
            
            // Deshabilitar botón para evitar múltiples clics
            disconnectSelectedButton.setEnabled(false);
            statusLabel.setText("Estado: Desconectando servidores...");
            statusLabel.setForeground(new Color(251, 191, 36));
            
            // Ejecutar desconexión en hilo separado para no bloquear UI
            Thread disconnectThread = new Thread(() -> {
                try {
                    logger.info("Iniciando desconexión de {} servidores", peerIds.size());
                    
                    // Desconectar usando backend real
                    int disconnected = serverNetworkControl.disconnectFromPeers(peerIds);
                    
                    logger.info("Desconexión completada: {} de {} servidores", disconnected, peerIds.size());
                    
                    // Actualizar UI en el Event Dispatch Thread
                    SwingUtilities.invokeLater(() -> {
                        try {
                            disconnectSelectedButton.setEnabled(true);
                            
                            if (disconnected > 0) {
                                statusLabel.setText("Estado: " + disconnected + " servidor(es) desconectado(s)");
                                statusLabel.setForeground(new Color(220, 38, 38));
                                showInfo("Se desconectaron " + disconnected + " servidor(es) exitosamente.");
                            } else {
                                statusLabel.setText("Estado: Error desconectando servidores");
                                statusLabel.setForeground(new Color(220, 38, 38));
                                showError("No se pudo desconectar ningún servidor.");
                            }
                            
                            tableModel.clearSelections();
                        } catch (Exception uiEx) {
                            logger.error("Error actualizando UI tras desconexión", uiEx);
                            disconnectSelectedButton.setEnabled(true);
                        }
                    });
                    
                } catch (Exception e) {
                    logger.error("Error durante proceso de desconexión", e);
                    
                    // Manejar errores en UI thread - SIEMPRE re-habilitar botón
                    SwingUtilities.invokeLater(() -> {
                        disconnectSelectedButton.setEnabled(true);
                        statusLabel.setText("Estado: Error inesperado desconectando");
                        statusLabel.setForeground(new Color(220, 38, 38));
                        showError("Error inesperado: " + e.getMessage());
                    });
                }
            }, "DisconnectPeersThread");
            
            disconnectThread.setDaemon(true); // No bloquear cierre de aplicación
            disconnectThread.start();
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

    // Método público para actualizar UI cuando P2P se inicia automáticamente
    public void updatePeerServerStatus(boolean isRunning) {
        SwingUtilities.invokeLater(() -> {
            if (isRunning) {
                int port = serverNetworkControl.getPeerServerPort();
                peerServerStatusLabel.setText("Estado P2P: Servidor activo en puerto " + port);
                peerServerStatusLabel.setForeground(new Color(16, 185, 129));
            } else {
                peerServerStatusLabel.setText("Estado P2P: Servidor detenido");
                peerServerStatusLabel.setForeground(new Color(220, 38, 38));
            }
        });
    }
    
    // PeerConnectionObserver implementation
    @Override
    public void onPeerConnected(ConnectedPeerInfo peer) {
        SwingUtilities.invokeLater(() -> {
            // Actualizar estado del servidor P2P si es la primera conexión
            if (tableModel.getRowCount() == 0 && serverNetworkControl.isPeerServerRunning()) {
                updatePeerServerStatus(true);
            }
            
            // Convertir de ConnectedPeerInfo a ConnectedServerInfo para la tabla
            ConnectedServerInfo serverInfo = new ConnectedServerInfo(
                peer.peerId(),
                peer.ipAddress(),
                peer.port(),
                peer.connectionTime(),
                true // isActive
            );
            
            tableModel.addServer(serverInfo);
            updateConnectedServersLabel();
            statusLabel.setText("Estado: Nuevo servidor conectado - " + peer.peerId());
            statusLabel.setForeground(new Color(34, 197, 94));
            
            // Mostrar mensaje de éxito solo cuando la conexión sea confirmada
            showInfo("Conexión establecida exitosamente con " + peer.peerId());
        });
    }

    @Override
    public void onPeerDisconnected(ConnectedPeerInfo peer) {
        SwingUtilities.invokeLater(() -> {
            // Buscar y remover el servidor por peerId
            for (int i = 0; i < tableModel.getRowCount(); i++) {
                ConnectedServerInfo server = tableModel.getServerAt(i);
                String serverPeerId = server.getIpAddress() + ":" + server.getPort();
                
                if (serverPeerId.equals(peer.peerId())) {
                    tableModel.removeServerAt(i);
                    updateConnectedServersLabel();
                    statusLabel.setText("Estado: Servidor desconectado - " + peer.peerId());
                    statusLabel.setForeground(new Color(220, 38, 38));
                    break;
                }
            }
        });
    }

    @Override
    public void onPeerStatusChanged(ConnectedPeerInfo peer) {
        SwingUtilities.invokeLater(() -> {
            // Actualizar estado del servidor en la tabla
            tableModel.updateServerStatus(peer.ipAddress(), peer.port(), peer.status());
            
            // Actualizar latencia si está disponible
            if (peer.latency() > 0) {
                tableModel.updateServerLatency(peer.ipAddress(), peer.port(), peer.latency());
            }
            
            statusLabel.setText("Estado: Cambio de estado - " + peer.peerId() + " : " + peer.status());
            statusLabel.setForeground(new Color(59, 130, 246));
        });
    }
    
    @Override
    public void onPeerConnectionError(String peerId, String errorMessage) {
        SwingUtilities.invokeLater(() -> {
            // Mostrar alert de error
            JOptionPane.showMessageDialog(
                this,
                "Error al conectar con el servidor P2P:\n" + peerId + "\n\n" + errorMessage,
                "Error de Conexión P2P",
                JOptionPane.ERROR_MESSAGE
            );
            
            // Actualizar status label
            statusLabel.setText("Estado: Error - " + errorMessage);
            statusLabel.setForeground(new Color(220, 38, 38));
        });
    }
}
