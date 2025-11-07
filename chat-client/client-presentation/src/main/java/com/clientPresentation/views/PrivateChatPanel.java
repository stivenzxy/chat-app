package com.clientPresentation.views;

import com.chatCommon.dto.MessageDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientApplication.commands.TranscribeAudioClientCommand;
import com.clientInfrastructure.persistence.dao.MessageDAO;
import com.clientPresentation.views.components.BaseChatPanel;
import com.clientPresentation.views.components.atoms.ChatMessagePanel;

import javax.swing.*;
import java.awt.Component;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PrivateChatPanel extends BaseChatPanel {
    private final String otherUsername;
    private final ClientCommand<MessageDTO, Boolean> sendMessageCommand;
    private final ClientCommand<MessageDTO, Boolean> sendAudioCommand;
    private final TranscribeAudioClientCommand transcribeAudioCommand;
    private final MessageDAO messageDAO;
    
    private final Set<String> recentlySentMessages = ConcurrentHashMap.newKeySet();

    public PrivateChatPanel(String selfUsername, String otherUsername, CommandFactory commandFactory) {
        super(selfUsername);
        this.otherUsername = otherUsername;
        this.sendMessageCommand = commandFactory.createSendPrivateMessageCommand();
        this.sendAudioCommand = commandFactory.createSendPrivateAudioCommand();
        this.transcribeAudioCommand = commandFactory.createTranscribeAudioCommand();
        this.messageDAO = new MessageDAO();
        loadChatHistory();
    }



    @Override
    protected void sendMessage() {
        String content = inputPanel.getMessageText().trim();
        if (content.isEmpty()) return;

        appendMessage(com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME, content, null);

        MessageDTO messageForNetwork = new MessageDTO(selfUsername, otherUsername, content);
        messageDAO.saveMessage(messageForNetwork);
        
        String messageKey = createMessageKey(messageForNetwork);
        recentlySentMessages.add(messageKey);
        
        new javax.swing.Timer(5000, e -> recentlySentMessages.remove(messageKey)).start();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                sendMessageCommand.execute(messageForNetwork);
                return null;
            }
        }.execute();

        inputPanel.clearMessageText();
    }

    @Override
    protected void sendAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) return;

        appendMessage(com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME, null, audioData);

        MessageDTO audioMessageForNetwork = new MessageDTO(selfUsername, otherUsername, audioData);
        messageDAO.saveMessage(audioMessageForNetwork);
        
        String messageKey = createMessageKey(audioMessageForNetwork);
        recentlySentMessages.add(messageKey);
        
        new javax.swing.Timer(5000, e -> recentlySentMessages.remove(messageKey)).start();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                sendAudioCommand.execute(audioMessageForNetwork);
                return null;
            }
        }.execute();
    }

    @Override
    protected String getRecipientId() {
        return otherUsername;
    }

    @Override
    protected void onTranscribeAudioAt(ChatMessagePanel sourcePanel, byte[] audioData) {
        if (hasTranscriptionAfterPanel(sourcePanel)) {
            return;
        }
        
        MessageDTO transcribingMessage = new MessageDTO("Sistema", "", "⏳ Transcribiendo audio...");
        ChatMessagePanel tempPanel = new ChatMessagePanel(transcribingMessage, audioService);
        int insertIndex = Math.min(chatHistoryArea.getComponentZOrder(sourcePanel) + 1, chatHistoryArea.getComponentCount());
        chatHistoryArea.add(tempPanel, insertIndex);
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();
        
        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return transcribeAudioCommand.execute(audioData);
            }

            @Override
            protected void done() {
                chatHistoryArea.remove(tempPanel);
                chatHistoryArea.revalidate();
                chatHistoryArea.repaint();
                
                try {
                    String transcribedText = get();
                    if (transcribedText != null && !transcribedText.trim().isEmpty()) {
                        if (!hasTranscriptionAfterPanel(sourcePanel)) {
                            MessageDTO transcriptionMessage = new MessageDTO("[TRANSCRIPCIÓN]", otherUsername, transcribedText);
                            ChatMessagePanel transcriptionPanel = new ChatMessagePanel(transcriptionMessage, audioService);
                            int idx = Math.min(chatHistoryArea.getComponentZOrder(sourcePanel) + 1, chatHistoryArea.getComponentCount());
                            chatHistoryArea.add(transcriptionPanel, idx);
                            chatHistoryArea.revalidate();
                            chatHistoryArea.repaint();
                        }
                    } else {
                        JOptionPane.showMessageDialog(PrivateChatPanel.this,
                            "No se pudo transcribir el audio. Verifique que el servidor Vosk esté ejecutándose.",
                            "Error de Transcripción",
                            JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PrivateChatPanel.this,
                        "Error al transcribir el audio: " + e.getMessage(),
                        "Error de Transcripción",
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }
    
    private boolean hasTranscriptionAfterPanel(ChatMessagePanel audioPanel) {
        int audioPanelIndex = chatHistoryArea.getComponentZOrder(audioPanel);
        if (audioPanelIndex < 0) {
            return false;
        }
        
        int nextIndex = audioPanelIndex + 1;
        if (nextIndex < chatHistoryArea.getComponentCount()) {
            Component nextComp = chatHistoryArea.getComponent(nextIndex);
            if (nextComp instanceof ChatMessagePanel) {
                ChatMessagePanel nextPanel = (ChatMessagePanel) nextComp;
                if (nextPanel.isTranscription()) {
                    return true;
                }
            }
        }
        
        return false;
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
                    loadHistoryMessages(history);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(PrivateChatPanel.this,
                            "Error al cargar el historial de chat.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void receiveMessage(MessageDTO message) {
        messageDAO.saveMessage(message);

        if (!historyLoaded) {
            messageQueue.add(message);
            return; // No intentar añadirlo a la UI todavía.
        }

        String displaySender = message.getSenderId().equals(selfUsername) 
            ? com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME 
            : message.getSenderId();
        
        if (message.getMessageType() == com.chatCommon.dto.MessageType.TEXT) {
            appendMessage(displaySender, message.getTextContent(), null);
        } else {
            appendMessage(displaySender, null, message.getAudioContent());
        }
    }
    
    public void displayEchoMessage(MessageDTO message) {
        messageDAO.saveMessage(message);
        
        String messageKey = createMessageKey(message);
        if (recentlySentMessages.contains(messageKey)) {
            return;
        }
        
        if (!historyLoaded) {
            messageQueue.add(message);
            return;
        }

        String displaySender = message.getSenderId().equals(selfUsername) ? com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME : message.getSenderId();
        if (message.getMessageType() == com.chatCommon.dto.MessageType.TEXT) {
            appendMessage(displaySender, message.getTextContent(), null);
        } else {
            appendMessage(displaySender, null, message.getAudioContent());
        }
    }
    
    private String createMessageKey(MessageDTO message) {
        if (message.getMessageType() == com.chatCommon.dto.MessageType.TEXT) {
            return message.getSenderId() + ":" + message.getRecipientId() + ":" + 
                   message.getTextContent() + ":" + message.getTimestamp().toLocalDate() + 
                   message.getTimestamp().toLocalTime().getHour() + message.getTimestamp().toLocalTime().getMinute();
        } else {
            return message.getSenderId() + ":" + message.getRecipientId() + ":AUDIO:" + 
                   message.getAudioContent().length + ":" + message.getTimestamp().toLocalDate() + 
                   message.getTimestamp().toLocalTime().getHour() + message.getTimestamp().toLocalTime().getMinute();
        }
    }
}