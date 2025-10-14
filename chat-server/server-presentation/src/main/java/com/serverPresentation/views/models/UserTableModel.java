package com.serverPresentation.views.models;

import com.serverApplication.dto.UserPresentationDTO;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class UserTableModel extends AbstractTableModel {
    
    private final String[] columnNames = {
        "ID", "Nombre de Usuario", "Email", "Dirección IP", "Fecha de Creación"
    };
    
    private List<UserPresentationDTO> users;
    
    public UserTableModel() {
        this.users = new ArrayList<>();
    }
    
    public UserTableModel(List<UserPresentationDTO> users) {
        this.users = users != null ? new ArrayList<>(users) : new ArrayList<>();
    }
    
    @Override
    public int getRowCount() {
        return users.size();
    }
    
    @Override
    public int getColumnCount() {
        return columnNames.length;
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
        
        return switch (columnIndex) {
            case 0 -> user.getId();
            case 1 -> user.getUsername();
            case 2 -> user.getEmail();
            case 3 -> user.getIpAddress() != null ? user.getIpAddress() : "N/A";
            case 4 -> user.getCreatedAt() != null ? user.getCreatedAt().toString() : "N/A";
            default -> null;
        };
    }
    
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // Tabla de solo lectura
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }
    
    public void setUsers(List<UserPresentationDTO> users) {
        this.users = users != null ? new ArrayList<>(users) : new ArrayList<>();
        fireTableDataChanged();
    }
    
    public void addUser(UserPresentationDTO user) {
        if (user != null) {
            users.add(user);
            fireTableRowsInserted(users.size() - 1, users.size() - 1);
        }
    }
    
    public void removeUser(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < users.size()) {
            users.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }
    
    public UserPresentationDTO getUserAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < users.size()) {
            return users.get(rowIndex);
        }
        return null;
    }
    
    public void clear() {
        int size = users.size();
        users.clear();
        if (size > 0) {
            fireTableRowsDeleted(0, size - 1);
        }
    }
    
    public List<UserPresentationDTO> getAllUsers() {
        return new ArrayList<>(users);
    }
}