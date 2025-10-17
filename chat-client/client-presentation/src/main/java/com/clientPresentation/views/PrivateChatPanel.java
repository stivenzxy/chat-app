package com.clientPresentation.views;

import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.MessageType;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.services.AudioService;
import com.clientInfrastructure.persistence.dao.MessageDAO;
import com.clientPresentation.views.components.atoms.ChatInputPanel;
import com.clientPresentation.views.components.atoms.ChatMessagePanel;

import javax.sound.sampled.LineUnavailableException;
import java.awt.event.ActionEvent;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class PrivateChatPanel extends JPanel {
    private final String selfUsername;
    private final String otherUsername;
    private final ClientCommand<MessageDTO, Boolean> sendMessageCommand;
    private final ClientCommand<MessageDTO, Boolean> sendAudioCommand;
    private final AudioService audioService;
    private final MessageDAO messageDAO;

    private JPanel chatHistoryArea;
    private ChatInputPanel inputPanel; // Usamos el nuevo componente

    public PrivateChatPanel(String selfUsername, String otherUsername, CommandFactory commandFactory) {
        this.selfUsername = selfUsername;
        this.otherUsername = otherUsername;
        this.sendMessageCommand = commandFactory.createSendPrivateMessageCommand();
        this.sendAudioCommand = commandFactory.createSendPrivateAudioCommand();
        this.audioService = new AudioService();
        this.messageDAO = new MessageDAO();
        initComponents();
        loadChatHistory();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatHistoryArea = new JPanel();
        chatHistoryArea.setLayout(new BoxLayout(chatHistoryArea, BoxLayout.Y_AXIS));
        chatHistoryArea.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(chatHistoryArea);
        add(scrollPane, BorderLayout.CENTER);

        inputPanel = new ChatInputPanel(); // Instanciamos el nuevo panel de entrada
        add(inputPanel, BorderLayout.SOUTH);

        // Acciones
        inputPanel.addSendAction(e -> sendMessage());
        inputPanel.getRecordButton().addActionListener(this::toggleRecording);
    }

    private void toggleRecording(ActionEvent e) {
        JToggleButton recordButton = inputPanel.getRecordButton();
        if (recordButton.isSelected()) {
            try {
                audioService.startRecording();
                recordButton.setText("⏹️");
                recordButton.setForeground(Color.RED);
            } catch (LineUnavailableException ex) {
                JOptionPane.showMessageDialog(this, "Error al acceder al micrófono: " + ex.getMessage(), "Error de Audio", JOptionPane.ERROR_MESSAGE);
                recordButton.setSelected(false);
            }
        } else {
            byte[] audioData = audioService.stopRecording();
            recordButton.setText("🎤");
            recordButton.setForeground(Color.WHITE);
            sendAudio(audioData);
        }
    }


    private void sendMessage() {
        String content = inputPanel.getMessageText().trim();
        if (content.isEmpty()) return;

        appendUIMessage(new MessageDTO("Yo", otherUsername, content));

        MessageDTO messageForNetwork = new MessageDTO(selfUsername, otherUsername, content);
        messageDAO.saveMessage(messageForNetwork);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                sendMessageCommand.execute(messageForNetwork);
                return null;
            }
        }.execute();

        inputPanel.clearMessageText();
    }

    private void sendAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) return;

        appendUIMessage(new MessageDTO("Yo", otherUsername, audioData));

        MessageDTO audioMessageForNetwork = new MessageDTO(selfUsername, otherUsername, audioData);
        messageDAO.saveMessage(audioMessageForNetwork);

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                sendAudioCommand.execute(audioMessageForNetwork);
                return null;
            }
        }.execute();
    }

    private void appendUIMessage(MessageDTO message) {
        SwingUtilities.invokeLater(() -> {
            chatHistoryArea.add(new ChatMessagePanel(message, audioService));
            chatHistoryArea.revalidate();
            chatHistoryArea.repaint();
        });
    }

    private void loadChatHistory() {
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                return messageDAO.getChatHistory(selfUsername, otherUsername);
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> history = get();
                    for (MessageDTO msg : history) {
                        if (msg.getSenderId().equals(selfUsername)) {
                            msg.setSenderId("Yo");
                        }
                        appendUIMessage(msg);
                    }
                } catch (Exception e) {
                    System.err.println("Error al cargar el historial de chat: " + e);
                    JOptionPane.showMessageDialog(PrivateChatPanel.this,
                            "Error al cargar el historial de chat.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void receiveMessage(MessageDTO message ) {
        messageDAO.saveMessage(message);
        appendUIMessage(message);
    }
}