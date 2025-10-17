package com.clientPresentation.views.components;

import com.chatCommon.dto.MessageDTO;
import com.clientPresentation.services.AudioService;
import com.clientPresentation.views.components.atoms.ChatInputPanel;
import com.clientPresentation.views.components.atoms.ChatMessagePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public abstract class BaseChatPanel extends JPanel {
    protected final String selfUsername;
    protected final AudioService audioService;
    protected JPanel chatHistoryArea;
    protected ChatInputPanel inputPanel;
    protected boolean historyLoaded = false;

    public BaseChatPanel(String selfUsername) {
        this.selfUsername = selfUsername;
        this.audioService = new AudioService();
        initComponents();
    }

    protected void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatHistoryArea = new JPanel();
        chatHistoryArea.setLayout(new BoxLayout(chatHistoryArea, BoxLayout.Y_AXIS));
        chatHistoryArea.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(chatHistoryArea);
        add(scrollPane, BorderLayout.CENTER);

        inputPanel = new ChatInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        // Common actions
        inputPanel.addSendAction(e -> sendMessage());
        inputPanel.getRecordButton().addActionListener(this::toggleRecording);
    }

    protected void toggleRecording(ActionEvent e) {
        JToggleButton recordButton = inputPanel.getRecordButton();
        if (recordButton.isSelected()) {
            try {
                audioService.startRecording();
                recordButton.setText(com.clientPresentation.views.constants.ChatConstants.AUDIO_BUTTON_STOP);
                recordButton.setForeground(Color.RED);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, com.clientPresentation.views.constants.ChatConstants.ERROR_AUDIO_ACCESS + ex.getMessage(), 
                    com.clientPresentation.views.constants.ChatConstants.ERROR_AUDIO_TITLE, JOptionPane.ERROR_MESSAGE);
                recordButton.setSelected(false);
            }
        } else {
            byte[] audioData = audioService.stopRecording();
            if (audioData != null && audioData.length > 0) {
                sendAudio(audioData);
            }
            recordButton.setText(com.clientPresentation.views.constants.ChatConstants.AUDIO_BUTTON_RECORD);
            recordButton.setForeground(Color.BLUE);
        }
    }

    protected void appendMessage(String sender, String text, byte[] audio) {
        MessageDTO message;
        String displaySender = sender.equals(selfUsername) ? com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME : sender;
        
        if (text != null && !text.trim().isEmpty()) {
            message = new MessageDTO(displaySender, getRecipientId(), text);
        } else if (audio != null && audio.length > 0) {
            message = new MessageDTO(displaySender, getRecipientId(), audio);
        } else {
            return;
        }
        
        ChatMessagePanel messagePanel = new ChatMessagePanel(message, audioService, this::onTranscribeAudio);
        chatHistoryArea.add(messagePanel);
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) chatHistoryArea.getParent().getParent();
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    protected void loadHistoryMessages(List<MessageDTO> messages) {
        chatHistoryArea.removeAll();
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();
        
        for (MessageDTO msg : messages) {
            String displaySender = msg.getSenderId().equals(selfUsername) ? com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME : msg.getSenderId();
            MessageDTO displayMessage;
            
            if (msg.getMessageType() == com.chatCommon.dto.MessageType.TEXT) {
                displayMessage = new MessageDTO(displaySender, getRecipientId(), msg.getTextContent());
            } else {
                displayMessage = new MessageDTO(displaySender, getRecipientId(), msg.getAudioContent());
            }
            
            ChatMessagePanel messagePanel = new ChatMessagePanel(displayMessage, audioService, this::onTranscribeAudio);
            chatHistoryArea.add(messagePanel);
        }
        
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();
        historyLoaded = true;
    }

    protected abstract void sendMessage();
    protected abstract void sendAudio(byte[] audioData);
    protected abstract String getRecipientId();
    
    // Method to handle audio transcription - can be overridden by subclasses
    protected void onTranscribeAudio(byte[] audioData) {
    }
}
