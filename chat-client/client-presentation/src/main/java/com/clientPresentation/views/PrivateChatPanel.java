package com.clientPresentation.views;

import com.chatCommon.dto.MessageDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.services.AudioService;
import com.clientInfrastructure.persistence.dao.MessageDAO;
import javax.sound.sampled.LineUnavailableException;
import java.awt.event.ActionEvent;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PrivateChatPanel extends JPanel {
    private final String selfUsername;
    private final String otherUsername;
    private final ClientCommand<MessageDTO, Boolean> sendMessageCommand;

    private JPanel  chatHistoryArea;
    private JTextField messageInputField;
    private JButton sendButton;

    private JToggleButton recordButton; // Cambiamos el botón por uno de tipo toggle
    private final AudioService audioService;
    private final ClientCommand<MessageDTO, Boolean> sendAudioCommand;

    private final MessageDAO messageDAO;

    public PrivateChatPanel(String selfUsername, String otherUsername, CommandFactory commandFactory) {
        this.selfUsername = selfUsername;
        this.otherUsername = otherUsername;
        this.sendMessageCommand = commandFactory.createSendPrivateMessageCommand();
        this.sendAudioCommand = commandFactory.createSendPrivateAudioCommand(); // Nuevo
        this.audioService = new AudioService(); // Nuevo
        this.messageDAO = new MessageDAO(); // Instanciar DAO
        initComponents();
        loadChatHistory();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Área de historial del chat
        chatHistoryArea = new JPanel();
        chatHistoryArea.setLayout(new BoxLayout(chatHistoryArea, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(chatHistoryArea);
        add(scrollPane, BorderLayout.CENTER);

        // Panel de entrada de mensajes
        JPanel inputPanel = new JPanel(new BorderLayout(5, 0));
        messageInputField = new JTextField();
        sendButton = new JButton("Enviar");

        // NUEVO BOTÓN DE GRABACIÓN
        recordButton = new JToggleButton("Grabar");

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(sendButton);
        buttonPanel.add(recordButton);

        inputPanel.add(messageInputField, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // Acciones
        sendButton.addActionListener(e -> sendMessage());
        messageInputField.addActionListener(e -> sendMessage());
        recordButton.addActionListener(this::toggleRecording);
    }

    private void toggleRecording(ActionEvent e) {
        if (recordButton.isSelected()) {
            // Empezar a grabar
            try {
                audioService.startRecording();
                recordButton.setText("Detener");
                recordButton.setForeground(Color.RED);
                messageInputField.setEnabled(false);
                sendButton.setEnabled(false);
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(this, "Error al acceder al micrófono: " + ex.getMessage(), "Error de Audio", JOptionPane.ERROR_MESSAGE);
                recordButton.setSelected(false);
            }
        } else {
            // Detener y enviar
            byte[] audioData = audioService.stopRecording();
            recordButton.setText("Grabar");
            recordButton.setForeground(Color.BLACK);

            sendAudio(audioData);

            messageInputField.setEnabled(true);
            sendButton.setEnabled(true);
        }
    }

    private void sendAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) return;

        appendMessage("Yo", "[Mensaje de audio enviado]");

        MessageDTO audioMessage = new MessageDTO(selfUsername, otherUsername, audioData);
        messageDAO.saveMessage(audioMessage);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return sendAudioCommand.execute(audioMessage);
            }
        }.execute();
    }

    public void appendAudioMessage(String sender, byte[] audioData) {
        JPanel audioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        audioPanel.add(new JLabel(String.format("[%s] ha enviado un audio:", sender)));
        JButton playButton = new JButton("▶️ Reproducir");
        playButton.addActionListener(e -> {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    audioService.playAudio(audioData);
                    return null;
                }
                @Override
                protected void done() {
                    try { get(); } catch (Exception ex) {
                        JOptionPane.showMessageDialog(PrivateChatPanel.this, "Error al reproducir audio.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
        audioPanel.add(playButton);

        chatHistoryArea.add(audioPanel);
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();

    }

    private void sendMessage() {
        String content = messageInputField.getText().trim();
        if (content.isEmpty()) {
            return;
        }

        // Mostrar el mensaje propio inmediatamente
        appendMessage("Yo", content);

        // Crear el DTO y ejecutar el comando
        MessageDTO message = new MessageDTO(selfUsername, otherUsername, content);

        messageDAO.saveMessage(message);

        // Ejecutar en segundo plano para no bloquear la UI
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return sendMessageCommand.execute(message);
            }
            @Override
            protected void done() {
                try {
                    if (!get()) {
                        appendMessage("Sistema", "Error: No se pudo enviar el mensaje.");
                    }
                } catch (Exception e) {
                    appendMessage("Sistema", "Error de comunicación: " + e.getMessage());
                }
            }
        }.execute();

        messageInputField.setText("");
    }

    public void appendMessage(String sender, String content) {
        SwingUtilities.invokeLater(() -> {
            JLabel messageLabel = new JLabel(String.format("[%s]: %s", sender, content));
            chatHistoryArea.add(messageLabel);

            chatHistoryArea.revalidate();
            chatHistoryArea.repaint();
        });
    }

    private void loadChatHistory() {
        // --- INICIO DE LA LÓGICA FALTANTE ---
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() throws Exception {
                // Obtener el historial de la base de datos
                return messageDAO.getChatHistory(selfUsername, otherUsername);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> history = get();
                    for (MessageDTO msg : history) {
                        String sender = msg.getSenderId().equals(selfUsername) ? "Yo" : msg.getSenderId();

                        if (msg.getMessageType() == com.chatCommon.dto.MessageType.TEXT) {
                            appendMessage(sender, msg.getTextContent());
                        } else if (msg.getMessageType() == com.chatCommon.dto.MessageType.AUDIO) {
                            appendAudioMessage(sender, msg.getAudioContent());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(PrivateChatPanel.this,
                            "Error al cargar el historial de chat.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
        // --- FIN DE LA LÓGICA FALTANTE ---
    }

    public void receiveMessage(String sender, String content) {
        MessageDTO message = new MessageDTO(sender, selfUsername, content);
        messageDAO.saveMessage(message); // Guardar mensaje recibido
        appendMessage(sender, content);  // Mostrar en la UI
    }

    public String getOtherUsername() {
        return otherUsername;
    }
}