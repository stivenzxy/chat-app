package com.serverPresentation.views.models;

import com.serverApplication.dto.UserPresentationDTO;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class UserTableModel extends AbstractTableModel {

    // --- INICIO DE LA MODIFICACIÓN 1 ---
    private final String[] columnNames = {
            "ID", "Nombre de Usuario", "Email", "Dirección IP", "Fecha de Creación", "Acción"
    };
    // --- FIN DE LA MODIFICACIÓN 1 ---

    private List<UserPresentationDTO> users;

    public UserTableModel() {
        this.users = new ArrayList<>();
    }

    @Override
    public int getRowCount() {
        return users.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length; // Se ajusta automáticamente
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= users.size()) {
            return null;
        }

        UserPresentationDTO user = users.get(rowIndex);

        // --- INICIO DE LA MODIFICACIÓN 2 ---
        return switch (columnIndex) {
            case 0 -> user.getId();
            case 1 -> user.getUsername();
            case 2 -> user.getEmail();
            case 3 -> user.getIpAddress() != null ? user.getIpAddress() : "N/A";
            case 4 -> user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A";
            case 5 -> "Ver Foto"; // El texto que aparecerá en el botón
            default -> null;
        };
        // --- FIN DE LA MODIFICACIÓN 2 ---
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    // --- INICIO DE NUEVO MÉTODO ---
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Hacemos que la columna de acción sea "editable" para que capture el clic del ratón
        return columnIndex == 5;
    }
    // --- FIN DE NUEVO MÉTODO ---

    public void setUsers(List<UserPresentationDTO> users) {
        this.users = users != null ? new ArrayList<>(users) : new ArrayList<>();
        fireTableDataChanged();
    }

    public UserPresentationDTO getUserAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < users.size()) {
            return users.get(rowIndex);
        }
        return null;
    }
}