package com.clientPresentation.views;

import com.chatCommon.dto.MessageDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientInfrastructure.persistence.dao.MessageDAO;
import com.clientPresentation.views.components.BaseChatPanel;

import javax.swing.*;
import java.util.List;

public class PrivateChatPanel extends BaseChatPanel {
    private final String otherUsername;
    private final ClientCommand<MessageDTO, Boolean> sendMessageCommand;
    private final ClientCommand<MessageDTO, Boolean> sendAudioCommand;
    private final MessageDAO messageDAO;

    public PrivateChatPanel(String selfUsername, String otherUsername, CommandFactory commandFactory) {
        super(selfUsername);
        this.otherUsername = otherUsername;
        this.sendMessageCommand = commandFactory.createSendPrivateMessageCommand();
        this.sendAudioCommand = commandFactory.createSendPrivateAudioCommand();
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

    public void receiveMessage(MessageDTO message ) {
        messageDAO.saveMessage(message);
        
        if (historyLoaded) {
            String displaySender = message.getSenderId().equals(selfUsername) ? com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME : message.getSenderId();
            if (message.getMessageType() == com.chatCommon.dto.MessageType.TEXT) {
                appendMessage(displaySender, message.getTextContent(), null);
            } else {
                appendMessage(displaySender, null, message.getAudioContent());
            }
        }
    }
}