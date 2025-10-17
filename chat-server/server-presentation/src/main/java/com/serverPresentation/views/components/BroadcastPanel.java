package com.serverPresentation.views.components;

import com.chatCommon.viewResources.UiBuilder;
import com.serverApplication.ports.ServerControl;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BroadcastPanel extends JPanel {
    
    private JTextArea messageArea;
    private JButton sendButton;
    private JTextArea historyArea;
    private JScrollPane historyScrollPane;
    private final ServerControl serverControl;
    
    public BroadcastPanel(ServerControl serverControl) {
        this.serverControl = serverControl;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel titlePanel = createTitlePanel();
        add(titlePanel, BorderLayout.NORTH);
        
        JPanel historyPanel = createHistoryPanel();
        add(historyPanel, BorderLayout.CENTER);
        
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);
    }
    
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createTitledBorder("Mensajes de Broadcast"));
        
        JLabel titleLabel = new JLabel("Enviar mensaje a todos los clientes conectados");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        titleLabel.setForeground(new Color(55, 65, 81));
        panel.add(titleLabel);
        
        return panel;
    }
    
    private JPanel createHistoryPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Historial de Broadcasts"));
        
        historyArea = new JTextArea();
        historyArea.setEditable(false);
        historyArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        historyArea.setBackground(new Color(248, 249, 250));
        historyArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        historyArea.setText("--- Historial de mensajes broadcast ---\n\n");
        
        historyScrollPane = new JScrollPane(historyArea);
        historyScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        historyScrollPane.setPreferredSize(new Dimension(0, 200));
        
        panel.add(historyScrollPane, BorderLayout.CENTER);
        return panel;
    }
    
    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Escribir Mensaje"));
        
        // Área de texto para el mensaje
        messageArea = new JTextArea(3, 0);
        messageArea.setLineWrap(true);
        messageArea.setWrapStyleWord(true);
        messageArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        messageArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(messageScrollPane, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        sendButton = new JButton("Enviar Broadcast");
        UiBuilder.styleButton(sendButton, new Color(59, 130, 246));
        sendButton.setPreferredSize(new Dimension(150, 35));
        sendButton.addActionListener(this::onSendBroadcast);
        
        JButton clearButton = new JButton("Limpiar");
        UiBuilder.styleButton(clearButton, new Color(107, 114, 128));
        clearButton.setPreferredSize(new Dimension(100, 35));
        clearButton.addActionListener(e -> messageArea.setText(""));
        
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);
        
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        // Enter para enviar (Ctrl+Enter)
        messageArea.getInputMap().put(KeyStroke.getKeyStroke("ctrl ENTER"), "send");
        messageArea.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSendBroadcast(e);
            }
        });
        
        return panel;
    }
    
    private void onSendBroadcast(ActionEvent e) {
        String message = messageArea.getText().trim();
        
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor, escriba un mensaje antes de enviarlo.",
                "Mensaje Vacío",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (message.length() > 500) {
            JOptionPane.showMessageDialog(this,
                "El mensaje es demasiado largo. Máximo 500 caracteres.",
                "Mensaje Muy Largo",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        try {
            // Enviar el broadcast usando el serverControl
            boolean success = serverControl.sendBroadcastMessage(message);
            
            if (success) {
                // Agregar al historial
                addToHistory("ENVIADO", message);
                
                // Limpiar el área de texto
                messageArea.setText("");
                
                // Mostrar confirmación
                JOptionPane.showMessageDialog(this,
                    "Mensaje broadcast enviado exitosamente a todos los clientes conectados.",
                    "Broadcast Enviado",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                    "Error al enviar el mensaje broadcast. Verifique que el servidor esté funcionando.",
                    "Error de Broadcast",
                    JOptionPane.ERROR_MESSAGE);
            }
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Error inesperado al enviar broadcast: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void addToHistory(String status, String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        
        String historyEntry = String.format("[%s] %s: %s\n\n", timestamp, status, message);
        historyArea.append(historyEntry);
        
        // Auto-scroll al final
        SwingUtilities.invokeLater(() -> {
            historyArea.setCaretPosition(historyArea.getDocument().getLength());
        });
    }
    
    public void setServerRunning(boolean isRunning) {
        sendButton.setEnabled(isRunning);
        messageArea.setEnabled(isRunning);
        
        if (!isRunning) {
            addToHistory("SISTEMA", "Servidor detenido - Broadcasting deshabilitado");
        } else {
            addToHistory("SISTEMA", "Servidor iniciado - Broadcasting habilitado");
        }
    }
}