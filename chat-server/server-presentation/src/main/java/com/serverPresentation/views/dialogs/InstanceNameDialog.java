package com.serverPresentation.views.dialogs;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo para capturar el nombre de la instancia del servidor
 * antes de iniciar el servidor y registrarlo en Eureka.
 */
public class InstanceNameDialog extends JDialog {
    private JTextField instanceNameField;
    private String instanceName;
    private boolean confirmed = false;
    
    public InstanceNameDialog(JFrame parent) {
        super(parent, "Nombre de Instancia del Servidor", true);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(450, 180);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        
        // Panel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Título
        JLabel titleLabel = new JLabel("Ingrese el nombre de esta instancia del servidor:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Campo de texto
        instanceNameField = new JTextField(20);
        instanceNameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        instanceNameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        instanceNameField.setText("chat-server");
        
        // Nota informativa
        JLabel noteLabel = new JLabel("<html><i>Este nombre identificará la instancia en Eureka</i></html>");
        noteLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        noteLabel.setForeground(Color.GRAY);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Agregar componentes con espaciado
        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(instanceNameField);
        mainPanel.add(Box.createVerticalStrut(5));
        mainPanel.add(noteLabel);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton confirmButton = new JButton("Confirmar");
        JButton cancelButton = new JButton("Cancelar");
        
        confirmButton.addActionListener(e -> confirm());
        cancelButton.addActionListener(e -> cancel());
        
        // Enter para confirmar
        instanceNameField.addActionListener(e -> confirm());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        
        // Agregar paneles al diálogo
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Estilo de botones
        confirmButton.setBackground(new Color(76, 175, 80));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFocusPainted(false);
        
        getRootPane().setDefaultButton(confirmButton);
    }
    
    private void confirm() {
        instanceName = instanceNameField.getText().trim();
        
        if (instanceName.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El nombre de la instancia no puede estar vacío",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Validar caracteres permitidos (alfanuméricos, guiones y guiones bajos)
        if (!instanceName.matches("^[a-zA-Z0-9-_]+$")) {
            JOptionPane.showMessageDialog(this,
                "El nombre solo puede contener letras, números, guiones y guiones bajos",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        confirmed = true;
        dispose();
    }
    
    private void cancel() {
        confirmed = false;
        dispose();
    }
    
    /**
     * Muestra el diálogo y retorna el nombre de la instancia ingresado
     * @return Nombre de la instancia o null si se canceló
     */
    public String showDialog() {
        setVisible(true);
        return confirmed ? instanceName : null;
    }
}
