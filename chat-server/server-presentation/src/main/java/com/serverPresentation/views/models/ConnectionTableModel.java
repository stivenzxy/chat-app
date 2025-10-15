package com.serverPresentation.views.models;

import com.serverApplication.dto.ConnectedClientInfo;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionTableModel extends AbstractTableModel {
    private final String[] columnNames = {"Seleccionar", "ID Cliente", "Dirección IP", "Estado"};
    private final List<ConnectedClientInfo> connections = new ArrayList<>();
    private final Map<ConnectedClientInfo, Boolean> selectedConnections = new HashMap<>();

    @Override
    public int getRowCount() { return connections.size(); }
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
        ConnectedClientInfo clientInfo = connections.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> selectedConnections.getOrDefault(clientInfo, false);
            case 1 -> clientInfo.id();
            case 2 -> clientInfo.ipAddress();
            case 3 -> "Conectado";
            default -> null;
        };
    }
    
    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && value instanceof Boolean) {
            ConnectedClientInfo clientInfo = connections.get(rowIndex);
            selectedConnections.put(clientInfo, (Boolean) value);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public void addConnection(ConnectedClientInfo clientInfo) {
        connections.add(clientInfo);
        selectedConnections.put(clientInfo, false);
        fireTableRowsInserted(connections.size() - 1, connections.size() - 1);
    }

    public void removeConnection(ConnectedClientInfo clientInfo) {
        for (int i = 0; i < connections.size(); i++) {
            ConnectedClientInfo existing = connections.get(i);
            System.out.println("  - [" + i + "] " + existing.id() + " | IP: " + existing.ipAddress());
        }

        int rowIndex = -1;
        for (int i = 0; i < connections.size(); i++) {
            if (connections.get(i).id().equals(clientInfo.id())) {
                rowIndex = i;
                break;
            }
        }

        if (rowIndex == -1) {
            for (int i = 0; i < connections.size(); i++) {
                ConnectedClientInfo existing = connections.get(i);
                boolean posibleTemporal = existing.id().startsWith("cliente-");
                if (posibleTemporal && existing.ipAddress().equals(clientInfo.ipAddress())) {
                    rowIndex = i;
                    break;
                }
            }
        }

        if (rowIndex != -1) {
            ConnectedClientInfo removedConnection = connections.remove(rowIndex);
            selectedConnections.remove(removedConnection);
            fireTableRowsDeleted(rowIndex, rowIndex);
        } else {
            System.out.println("DEBUG: No se encontró el cliente " + clientInfo.id() + " en la tabla para remover");
        }
    }
    
    public List<ConnectedClientInfo> getSelectedConnections() {
        return connections.stream()
                .filter(conn -> selectedConnections.getOrDefault(conn, false))
                .toList();
    }
    
    public void clearSelections() {
        selectedConnections.replaceAll((k, v) -> false);
        fireTableDataChanged();
    }
}