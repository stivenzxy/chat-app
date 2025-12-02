package com.serverPresentation.views.dialogs;

import javax.swing.*;
import java.awt.*;


public class InstanceNameDialog extends JDialog {
    private static final String SERVER_PREFIX = "chat-server-";
    private JTextField serverNumberField;
    private String instanceName;
    private boolean confirmed = false;
    
    public InstanceNameDialog(JFrame parent) {
        super(parent, "Nombre de Instancia del Servidor", true);
        setAlwaysOnTop(true);
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setSize(550, 220);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setResizable(false);
       
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JLabel titleLabel = new JLabel("Ingrese un número identificador para su servidor:");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
        inputPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 35));
        
        JLabel prefixLabel = new JLabel(SERVER_PREFIX);
        prefixLabel.setFont(new Font("Arial", Font.BOLD, 14));
        prefixLabel.setForeground(new Color(33, 150, 243));
        
        serverNumberField = new JTextField(10);
        serverNumberField.setFont(new Font("Arial", Font.PLAIN, 14));
        serverNumberField.setPreferredSize(new Dimension(150, 30));
        serverNumberField.setText("1");
        serverNumberField.setHorizontalAlignment(JTextField.CENTER);
        
        inputPanel.add(prefixLabel);
        inputPanel.add(serverNumberField);
        
        JLabel noteLabel = new JLabel("<html><i>Este identificador formará el nombre completo: chat-server-[número]</i></html>");
        noteLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        noteLabel.setForeground(Color.GRAY);
        noteLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        mainPanel.add(titleLabel);
        mainPanel.add(Box.createVerticalStrut(15));
        mainPanel.add(inputPanel);
        mainPanel.add(Box.createVerticalStrut(10));
        mainPanel.add(noteLabel);
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        
        JButton confirmButton = new JButton("Confirmar");
        JButton cancelButton = new JButton("Cancelar");
        
        confirmButton.addActionListener(e -> confirm());
        cancelButton.addActionListener(e -> cancel());
        
        serverNumberField.addActionListener(e -> confirm());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
 
        add(mainPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        confirmButton.setBackground(new Color(76, 175, 80));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setFocusPainted(false);
        
        getRootPane().setDefaultButton(confirmButton);
    }
    
    private void confirm() {
        String serverNumber = serverNumberField.getText().trim();
        
        if (serverNumber.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "El número identificador no puede estar vacío",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!serverNumber.matches("^[0-9]+$")) {
            JOptionPane.showMessageDialog(this,
                "El identificador debe ser solo números",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        instanceName = SERVER_PREFIX + serverNumber;
        confirmed = true;
        dispose();
    }
    
    private void cancel() {
        confirmed = false;
        dispose();
    }
    
    public String showDialog() {
        setVisible(true);
        return confirmed ? instanceName : null;
    }
}
