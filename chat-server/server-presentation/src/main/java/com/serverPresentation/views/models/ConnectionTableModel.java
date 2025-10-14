package com.serverPresentation.views.models;

import com.serverApplication.dto.ConnectedClientInfo;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ConnectionTableModel extends AbstractTableModel {
    private final String[] columnNames = {"ID Cliente", "Dirección IP", "Estado"};
    private final List<ConnectedClientInfo> connections = new ArrayList<>();

    @Override
    public int getRowCount() { return connections.size(); }
    @Override
    public int getColumnCount() { return columnNames.length; }
    @Override
    public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ConnectedClientInfo clientInfo = connections.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> clientInfo.id();
            case 1 -> clientInfo.ipAddress();
            case 2 -> "Conectado";
            default -> null;
        };
    }

    public void addConnection(ConnectedClientInfo clientInfo) {
        connections.add(clientInfo);
        fireTableRowsInserted(connections.size() - 1, connections.size() - 1);
    }

    public void removeConnection(ConnectedClientInfo clientInfo) {
        int rowIndex = connections.indexOf(clientInfo);
        if (rowIndex != -1) {
            connections.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }
}