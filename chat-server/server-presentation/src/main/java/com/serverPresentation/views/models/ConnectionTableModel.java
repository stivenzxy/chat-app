package com.serverPresentation.views.models;

import com.serverInfrastructure.network.ClientConnection;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class ConnectionTableModel extends AbstractTableModel {
    private final String[] columnNames = {"ID Cliente", "Dirección IP", "Estado"};
    private final List<ClientConnection> connections = new ArrayList<>();

    @Override
    public int getRowCount() { return connections.size(); }
    @Override
    public int getColumnCount() { return columnNames.length; }
    @Override
    public String getColumnName(int column) { return columnNames[column]; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ClientConnection connection = connections.get(rowIndex);
        return switch (columnIndex) {
            case 0 -> connection.id();
            case 1 -> connection.ipAddress();
            case 2 -> "Conectado";
            default -> null;
        };
    }

    public void addConnection(ClientConnection connection) {
        connections.add(connection);
        fireTableRowsInserted(connections.size() - 1, connections.size() - 1);
    }

    public void removeConnection(ClientConnection connection) {
        int rowIndex = connections.indexOf(connection);
        if (rowIndex != -1) {
            connections.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }
}