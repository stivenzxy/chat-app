package com.clientPresentation.views.components;

import com.chatCommon.dto.ChannelDTO;
import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.MessageType;
import com.clientApplication.commands.GetChannelMembersClientCommand;
import com.clientApplication.commands.GetChannelHistoryClientCommand;
import com.clientApplication.commands.SendChannelAudioClientCommand;
import com.clientApplication.commands.SendChannelMessageClientCommand;
import com.clientApplication.commands.TranscribeAudioClientCommand;
import com.clientInfrastructure.services.ChannelMemberService;
import com.clientInfrastructure.services.ChannelMessageService;
import com.clientPresentation.services.AudioService;
import com.clientPresentation.views.components.atoms.ChatInputPanel;
import com.clientPresentation.views.components.atoms.ChatMessagePanel;
import com.clientPresentation.views.constants.ChatConstants;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChannelChatPanel extends JPanel {
    private final String selfUsername;
    private final ChannelDTO channel;
    private final SendChannelMessageClientCommand sendMsgCmd;
    private final SendChannelAudioClientCommand sendAudioCmd;
    private final TranscribeAudioClientCommand transcribeAudioCmd;
    private final GetChannelHistoryClientCommand getHistoryCmd;
    private final ChannelMessageService messageService;
    private final ChannelMemberService memberService;
    private final AudioService audioService = new AudioService();
    private final DefaultListModel<String> membersModel = new DefaultListModel<>();
    private final JList<String> membersList = new JList<>(membersModel);

    private JPanel chatHistoryArea;
    private ChatInputPanel inputPanel;
    private boolean historyLoaded = false;

    public ChannelChatPanel(String selfUsername, ChannelDTO channel,
            SendChannelMessageClientCommand sendMsgCmd,
            SendChannelAudioClientCommand sendAudioCmd,
            GetChannelMembersClientCommand getMembersCmd,
            GetChannelHistoryClientCommand getHistoryCmd,
            TranscribeAudioClientCommand transcribeAudioCmd) {
        this.selfUsername = selfUsername;
        this.channel = channel;
        this.sendMsgCmd = sendMsgCmd;
        this.sendAudioCmd = sendAudioCmd;
        this.getHistoryCmd = getHistoryCmd;
        this.transcribeAudioCmd = transcribeAudioCmd;
        this.messageService = new ChannelMessageService();
        this.memberService = new ChannelMemberService(getMembersCmd);

        initComponents();
        loadHistory();
        loadMembers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel chatPanel = new JPanel(new BorderLayout(5, 5));

        chatHistoryArea = new JPanel();
        chatHistoryArea.setLayout(new BoxLayout(chatHistoryArea, BoxLayout.Y_AXIS));
        chatHistoryArea.setBackground(Color.WHITE);
        JScrollPane scrollPane = new JScrollPane(chatHistoryArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        chatPanel.add(scrollPane, BorderLayout.CENTER);

        inputPanel = new ChatInputPanel();
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        JPanel membersPanel = new JPanel(new BorderLayout(5, 5));
        membersPanel.setBorder(BorderFactory.createTitledBorder("Miembros"));
        membersList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof String username) {
                    setText("👤 " + username);
                }
                return c;
            }
        });
        JScrollPane membersScroll = new JScrollPane(membersList);
        membersScroll.setPreferredSize(new Dimension(150, 0));
        membersPanel.add(membersScroll, BorderLayout.CENTER);

        add(chatPanel, BorderLayout.CENTER);
        add(membersPanel, BorderLayout.EAST);

        inputPanel.addSendAction(e -> sendText());
        inputPanel.getRecordButton().addActionListener(this::toggleRecording);
    }

    private void loadHistory() {
        new SwingWorker<List<MessageDTO>, Void>() {
            @Override
            protected List<MessageDTO> doInBackground() {
                // 1. Try to fetch from server
                try {
                    List<MessageDTO> serverMessages = getHistoryCmd.execute(channel.getId());
                    if (serverMessages != null && !serverMessages.isEmpty()) {
                        // 2. Update local DB with new messages
                        for (MessageDTO msg : serverMessages) {
                            if (msg.getMessageType() == MessageType.TEXT) {
                                messageService.saveChannelTextMessage(msg.getSenderId(), channel.getId(),
                                        msg.getTextContent());
                            } else if (msg.getMessageType() == MessageType.AUDIO) {
                                messageService.saveChannelAudioMessage(msg.getSenderId(), channel.getId(),
                                        msg.getAudioContent());
                            }
                        }
                        return serverMessages;
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching history from server: " + e.getMessage());
                }

                // 3. Fallback to local DB if server fails or returns empty (and we want to show
                // what we have)
                return messageService.getChannelHistory(channel.getId());
            }

            @Override
            protected void done() {
                try {
                    List<MessageDTO> messages = get();
                    chatHistoryArea.removeAll();
                    for (MessageDTO m : messages) {
                        String displaySender = m.getSenderId().equals(selfUsername) ? ChatConstants.SELF_DISPLAY_NAME
                                : m.getSenderId();
                        MessageDTO displayMessage;

                        if (m.getMessageType() == MessageType.TEXT) {
                            displayMessage = new MessageDTO(displaySender, m.getRecipientId(), m.getTextContent());
                        } else {
                            displayMessage = new MessageDTO(displaySender, m.getRecipientId(), m.getAudioContent());
                        }

                        ChatMessagePanel messagePanel = new ChatMessagePanel(displayMessage, audioService,
                                ChannelChatPanel.this::onTranscribeAudioAt);
                        chatHistoryArea.add(messagePanel);
                    }
                    chatHistoryArea.revalidate();
                    chatHistoryArea.repaint();
                    historyLoaded = true;

                    // Scroll to bottom
                    SwingUtilities.invokeLater(() -> {
                        JScrollPane scrollPane = (JScrollPane) chatHistoryArea.getParent().getParent();
                        if (scrollPane != null) {
                            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
                        }
                    });

                } catch (Exception e) {
                    System.err.println("Error: " + e);
                }
            }
        }.execute();
    }

    private void loadMembers() {
        new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return memberService.getChannelMembers(channel.getId());
            }

            @Override
            protected void done() {
                try {
                    List<String> members = get();
                    membersModel.clear();
                    for (String member : members) {
                        membersModel.addElement(member);
                    }
                } catch (Exception e) {
                    System.err.println("Error: " + e);
                }
            }
        }.execute();
    }

    private void sendText() {
        String content = inputPanel.getMessageText().trim();
        if (content.isEmpty()) {
            return;
        }

        try {
            SendChannelMessageClientCommand.Request request = new SendChannelMessageClientCommand.Request(
                    channel.getId(), content);
            boolean result = sendMsgCmd.execute(request);

            if (result) {
                append(selfUsername, content, null);
                messageService.saveChannelTextMessage(selfUsername, channel.getId(), content);
                inputPanel.clearMessageText();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al enviar mensaje: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleRecording(java.awt.event.ActionEvent e) {
        javax.swing.JToggleButton recordButton = inputPanel.getRecordButton();
        if (recordButton.isSelected()) {
            try {
                audioService.startRecording();
                recordButton.setText(com.clientPresentation.views.constants.ChatConstants.AUDIO_BUTTON_STOP);
                recordButton.setForeground(Color.RED);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        com.clientPresentation.views.constants.ChatConstants.ERROR_AUDIO_ACCESS + ex.getMessage(),
                        com.clientPresentation.views.constants.ChatConstants.ERROR_AUDIO_TITLE,
                        JOptionPane.ERROR_MESSAGE);
                recordButton.setSelected(false);
            }
        } else {
            byte[] audioData = audioService.stopRecording();
            recordButton.setText(com.clientPresentation.views.constants.ChatConstants.AUDIO_BUTTON_RECORD);
            recordButton.setForeground(Color.WHITE);

            if (audioData != null && audioData.length > 0) {
                sendAudio(audioData);
            }
        }
    }

    private void sendAudio(byte[] data) {
        try {
            SendChannelAudioClientCommand.Request request = new SendChannelAudioClientCommand.Request(channel.getId(),
                    data);

            if (sendAudioCmd.execute(request)) {
                append(selfUsername, null, data);
                messageService.saveChannelAudioMessage(selfUsername, channel.getId(), data);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al enviar audio: " + e.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void append(String sender, String text, byte[] audio) {
        MessageDTO message;

        String displaySender = sender.equals(selfUsername)
                ? com.clientPresentation.views.constants.ChatConstants.SELF_DISPLAY_NAME
                : sender;

        if (text != null) {
            message = new MessageDTO(displaySender, channel.getId().toString(), text);
        } else if (audio != null) {
            message = new MessageDTO(displaySender, channel.getId().toString(), audio);
        } else {
            return;
        }

        ChatMessagePanel messagePanel = new ChatMessagePanel(message, audioService, this::onTranscribeAudioAt);

        chatHistoryArea.add(messagePanel);
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();

        SwingUtilities.invokeLater(() -> {
            JScrollPane scrollPane = (JScrollPane) chatHistoryArea.getParent().getParent();
            scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
        });
    }

    public void receiveText(String sender, String text) {
        messageService.saveChannelTextMessage(sender, channel.getId(), text);

        if (sender.equals(selfUsername)) {
            return;
        }

        if (historyLoaded) {
            append(sender, text, null);
        }
    }

    public void receiveAudio(String sender, String audioBase64) {
        try {
            byte[] data = java.util.Base64.getDecoder().decode(audioBase64);
            messageService.saveChannelAudioMessage(sender, channel.getId(), data);

            if (sender.equals(selfUsername)) {
                return;
            }

            if (historyLoaded) {
                append(sender, null, data);
            }
        } catch (IllegalArgumentException ignored) {
        }
    }

    public void refreshMembers() {
        loadMembers();
    }

    public ChannelDTO getChannel() {
        return channel;
    }

    public void onTranscribeAudioAt(ChatMessagePanel sourcePanel, byte[] audioData) {
        if (hasTranscriptionAfterPanel(sourcePanel)) {
            return;
        }

        MessageDTO transcribingMessage = new MessageDTO("Sistema", "", "⏳ Transcribiendo audio...");
        ChatMessagePanel tempPanel = new ChatMessagePanel(transcribingMessage, audioService);
        int insertIndex = Math.min(chatHistoryArea.getComponentZOrder(sourcePanel) + 1,
                chatHistoryArea.getComponentCount());
        chatHistoryArea.add(tempPanel, insertIndex);
        chatHistoryArea.revalidate();
        chatHistoryArea.repaint();

        new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() throws Exception {
                return transcribeAudioCmd.execute(audioData);
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
                            MessageDTO transcriptionMessage = new MessageDTO("[TRANSCRIPCIÓN]",
                                    channel.getId().toString(), transcribedText);
                            ChatMessagePanel transcriptionPanel = new ChatMessagePanel(transcriptionMessage,
                                    audioService);
                            int idx = Math.min(chatHistoryArea.getComponentZOrder(sourcePanel) + 1,
                                    chatHistoryArea.getComponentCount());
                            chatHistoryArea.add(transcriptionPanel, idx);
                            chatHistoryArea.revalidate();
                            chatHistoryArea.repaint();
                        }
                    } else {
                        JOptionPane.showMessageDialog(ChannelChatPanel.this,
                                "No se pudo transcribir el audio. Verifique que el servidor Vosk esté ejecutándose.",
                                "Error de Transcripción",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ChannelChatPanel.this,
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
}
