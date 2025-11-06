package com.serverPresentation.views.models;

import javax.swing.table.AbstractTableModel;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerNetworkTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Seleccionar", "IP", "Puerto", "Estado", "Fecha Conexión", "Latencia (ms)"};
    private final List<ConnectedServerInfo> servers = new ArrayList<>();
    private final Map<ConnectedServerInfo, Boolean> selectedServers = new HashMap<>();
    
    public static class ConnectedServerInfo {
        private final String ipAddress;
        private final int port;
        private final String status;
        private final LocalDateTime connectionTime;
        private int latency;
        
        public ConnectedServerInfo(String ipAddress, int port, String status, LocalDateTime connectionTime) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.status = status;
            this.connectionTime = connectionTime;
            this.latency = 0;
        }
        
        // Constructor adicional para compatibilidad con ConnectedPeerInfo
        public ConnectedServerInfo(String peerId, String ipAddress, int port, LocalDateTime connectionTime, boolean isActive) {
            this.ipAddress = ipAddress;
            this.port = port;
            this.status = isActive ? "Activo" : "Inactivo";
            this.connectionTime = connectionTime;
            this.latency = 0;
        }
        
        public String getIpAddress() { return ipAddress; }
        public int getPort() { return port; }
        public String getStatus() { return status; }
        public LocalDateTime getConnectionTime() { return connectionTime; }
        public int getLatency() { return latency; }
        public void setLatency(int latency) { this.latency = latency; }
        
        public String getConnectionKey() {
            return ipAddress + ":" + port;
        }
    }
    
    @Override
    public int getRowCount() { return servers.size(); }
    
    @Override
    public int getColumnCount() { return columnNames.length; }
    
    @Override
    public String getColumnName(int column) { return columnNames[column]; }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> Boolean.class;
            default -> String.class;
        };
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0;
    }
    
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ConnectedServerInfo serverInfo = servers.get(rowIndex);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        return switch (columnIndex) {
            case 0 -> selectedServers.getOrDefault(serverInfo, false);
            case 1 -> serverInfo.getIpAddress();
            case 2 -> String.valueOf(serverInfo.getPort());
            case 3 -> serverInfo.getStatus();
            case 4 -> serverInfo.getConnectionTime().format(formatter);
            case 5 -> serverInfo.getLatency() > 0 ? String.valueOf(serverInfo.getLatency()) : "-";
            default -> null;
        };
    }
    
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && value instanceof Boolean) {
            ConnectedServerInfo serverInfo = servers.get(rowIndex);
            selectedServers.put(serverInfo, (Boolean) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }
    
    public void addServer(ConnectedServerInfo serverInfo) {
        servers.add(serverInfo);
        selectedServers.put(serverInfo, false);
        fireTableRowsInserted(servers.size() - 1, servers.size() - 1);
    }
    
    public void removeServer(String ipAddress, int port) {
        int rowIndex = -1;
        for (int i = 0; i < servers.size(); i++) {
            ConnectedServerInfo info = servers.get(i);
            if (info.getIpAddress().equals(ipAddress) && info.getPort() == port) {
                rowIndex = i;
                break;
            }
        }
        if (rowIndex != -1) {
            ConnectedServerInfo removed = servers.remove(rowIndex);
            selectedServers.remove(removed);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }
    
    public void removeServerAt(int index) {
        if (index >= 0 && index < servers.size()) {
            ConnectedServerInfo removed = servers.remove(index);
            selectedServers.remove(removed);
            fireTableRowsDeleted(index, index);
        }
    }
    
    public ConnectedServerInfo getServerAt(int index) {
        if (index >= 0 && index < servers.size()) {
            return servers.get(index);
        }
        return null;
    }
    
    public void updateServerStatus(String ipAddress, int port, String status) {
        for (int i = 0; i < servers.size(); i++) {
            ConnectedServerInfo info = servers.get(i);
            if (info.getIpAddress().equals(ipAddress) && info.getPort() == port) {
                fireTableRowsUpdated(i, i);
                break;
            }
        }
    }
    
    public void updateServerLatency(String ipAddress, int port, int latency) {
        for (int i = 0; i < servers.size(); i++) {
            ConnectedServerInfo info = servers.get(i);
            if (info.getIpAddress().equals(ipAddress) && info.getPort() == port) {
                info.setLatency(latency);
                fireTableRowsUpdated(i, i);
                break;
            }
        }
    }
    
    public List<ConnectedServerInfo> getSelectedServers() {
        return servers.stream()
                .filter(server -> selectedServers.getOrDefault(server, false))
                .toList();
    }
    
    public void clearSelections() {
        selectedServers.replaceAll((k, v) -> false);
        fireTableDataChanged();
    }
    
    public void clearAllServers() {
        int size = servers.size();
        if (size > 0) {
            servers.clear();
            selectedServers.clear();
            fireTableRowsDeleted(0, size - 1);
        }
    }
    
    public List<ConnectedServerInfo> getAllServers() {
        return new ArrayList<>(servers);
    }
}
