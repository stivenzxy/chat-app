package com.clientPresentation.views.components.atoms;

import com.chatCommon.viewResources.UiBuilder;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionListener;

public class ChatInputPanel extends JPanel {

    private final JTextField messageInputField;
    private final JButton sendButton;
    private final JToggleButton recordButton;

    public ChatInputPanel() {
        setLayout(new BorderLayout(5, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        messageInputField = new JTextField();
        sendButton = new JButton("⌯⌲");
        recordButton = new JToggleButton("🎤");

        Border roundedBorder = UiBuilder.createRoundedBorder();
        UiBuilder.styleField(messageInputField, roundedBorder);

        styleIconButton(sendButton, new Color(13, 149, 28));
        styleIconButton(recordButton, new Color(59, 130, 246));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.add(recordButton);
        buttonPanel.add(sendButton);

        add(messageInputField, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.EAST);
    }

    public String getMessageText() { return messageInputField.getText(); }

    public void clearMessageText() { messageInputField.setText(""); }

    public JToggleButton getRecordButton() { return recordButton; }

    public void addSendAction(ActionListener listener) {
        sendButton.addActionListener(e -> {
            listener.actionPerformed(e);
        });
        messageInputField.addActionListener(e -> {
            listener.actionPerformed(e);
        });
    }

    private void styleIconButton(AbstractButton button, Color color) {
        button.setFont(UiBuilder.BUTTON_FONT.deriveFont(16f));
        button.setPreferredSize(new Dimension(50, 40));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new com.chatCommon.viewResources.RoundBorder(8, color.darker()));
    }
}

