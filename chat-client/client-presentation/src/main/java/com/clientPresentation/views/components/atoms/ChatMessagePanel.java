package com.clientPresentation.views.components.atoms;

import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.MessageType;
import com.chatCommon.viewResources.UiBuilder;
import com.clientPresentation.services.AudioService;

import javax.swing.*;
import java.awt.*;

public class ChatMessagePanel extends JPanel {

    public ChatMessagePanel(MessageDTO message, AudioService audioService) {
        super(new BorderLayout());
        setAlignmentX(Component.CENTER_ALIGNMENT);

        setOpaque(true);
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        final String userColor = message.getSenderId().equals("Yo") ? "#007BFF" : "#28A745"; // Azul para "Yo", Verde para el otro

        if (message.getMessageType() == MessageType.TEXT) {
            String htmlContent = String.format(
                    "<html><b style='color:%s;'>%s:</b> %s</html>",
                    userColor,
                    message.getSenderId(),
                    message.getTextContent()
            );
            JLabel messageLabel = new JLabel(htmlContent);
            add(messageLabel, BorderLayout.CENTER);
        } else if (message.getMessageType() == MessageType.AUDIO) {
            add(createAudioMessagePanel(message.getSenderId(), userColor, message.getAudioContent(), audioService), BorderLayout.CENTER);
        }
    }

    private JPanel createAudioMessagePanel(String sender, String userColor,byte[] audioData, AudioService audioService) {
        JPanel audioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        audioPanel.setOpaque(false);
        String htmlContent = String.format(
                "<html><b style='color:%s;'>%s:</b> <i>Mensaje de audio</i></html>",
                userColor,
                sender
        );

        audioPanel.add(new JLabel(htmlContent));

        JButton playButton = new JButton("▶️ Reproducir");
        UiBuilder.styleButton(playButton, new Color(107, 114, 128));
        playButton.setPreferredSize(new Dimension(140, 30));

        playButton.addActionListener(e -> new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                audioService.playAudio(audioData);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(
                            ChatMessagePanel.this,
                            "Error al reproducir el audio: " + e.getCause().getMessage(),
                            "Error de Audio",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute());

        audioPanel.add(playButton);
        return audioPanel;
    }
}