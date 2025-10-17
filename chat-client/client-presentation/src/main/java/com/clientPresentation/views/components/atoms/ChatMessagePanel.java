package com.clientPresentation.views.components.atoms;

import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.MessageType;
import com.chatCommon.viewResources.UiBuilder;
import com.clientPresentation.services.AudioService;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.awt.*;
import java.util.function.Consumer;

public class ChatMessagePanel extends JPanel {

    public ChatMessagePanel(MessageDTO message, AudioService audioService) {
        this(message, audioService, null);
    }

    public ChatMessagePanel(MessageDTO message, AudioService audioService, Consumer<byte[]> onTranscribeRequest) {
        super(new BorderLayout());
        setAlignmentX(Component.CENTER_ALIGNMENT);

        setOpaque(true);
        setBackground(new Color(240, 240, 240));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        final String userColor = message.getSenderId().equals("Yo") ? "#007BFF" : "#28A745";

        if (message.getMessageType() == MessageType.TEXT) {
            JTextPane textPane = createTextMessagePane(message.getSenderId(), userColor, message.getTextContent());
            
            JPanel textContainer = new JPanel(new BorderLayout()) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension parentSize = getParent() != null ? getParent().getSize() : new Dimension(400, 100);
                    int maxWidth = Math.max(200, parentSize.width - 40);
                    
                    textPane.setSize(maxWidth, Short.MAX_VALUE);
                    Dimension textSize = textPane.getPreferredSize();
                    
                    return new Dimension(maxWidth, Math.max(textSize.height, 20));
                }
                
                @Override
                public void doLayout() {
                    super.doLayout();
                    if (getParent() != null) {
                        int maxWidth = Math.max(200, getParent().getWidth() - 40);
                        textPane.setSize(maxWidth, getHeight());
                        textPane.revalidate();
                    }
                }
            };
            
            textContainer.setOpaque(false);
            textContainer.add(textPane, BorderLayout.CENTER);
            
            add(textContainer, BorderLayout.CENTER);
        } else if (message.getMessageType() == MessageType.AUDIO) {
            add(createAudioMessagePanel(message.getSenderId(), userColor, message.getAudioContent(), audioService, onTranscribeRequest), BorderLayout.CENTER);
        }
    }

    private JPanel createAudioMessagePanel(String sender, String userColor, byte[] audioData, AudioService audioService, Consumer<byte[]> onTranscribeRequest) {
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

        playButton.addActionListener(e -> {
            playButton.setEnabled(false);
            playButton.setText("🔊 Reproduciendo");
            
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    audioService.playAudio(audioData);
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        // Reproducción exitosa
                    } catch (Exception ex) {
                        String errorMessage = "Error al reproducir el audio";
                        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                            errorMessage += ": " + ex.getCause().getMessage();
                        }
                        JOptionPane.showMessageDialog(
                                ChatMessagePanel.this,
                                errorMessage,
                                "Error de Audio",
                                JOptionPane.ERROR_MESSAGE
                        );
                    } finally {
                        // Restaurar el botón
                        playButton.setEnabled(true);
                        playButton.setText("▶️ Reproducir");
                    }
                }
            }.execute();
        });

        audioPanel.add(playButton);

        // Agregar botón de transcripción si hay callback
        if (onTranscribeRequest != null) {
            JButton transcribeButton = new JButton("📝 Transcribir");
            UiBuilder.styleButton(transcribeButton, new Color(52, 144, 220));
            transcribeButton.setPreferredSize(new Dimension(140, 30));

            transcribeButton.addActionListener(e -> {
                transcribeButton.setEnabled(false);
                transcribeButton.setText("⏳ Transcribiendo...");
                
                new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        onTranscribeRequest.accept(audioData);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(
                                    ChatMessagePanel.this,
                                    "Error al transcribir el audio: " + ex.getMessage(),
                                    "Error de Transcripción",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        } finally {
                            transcribeButton.setEnabled(true);
                            transcribeButton.setText("📝 Transcribir");
                        }
                    }
                }.execute();
            });

            audioPanel.add(transcribeButton);
        }

        return audioPanel;
    }
    
    /**
     * Crea un JTextPane para mostrar mensajes de texto con formato y ajuste automático
     */
    private JTextPane createTextMessagePane(String sender, String userColor, String textContent) {
        JTextPane textPane = new JTextPane() {
            @Override
            public Dimension getPreferredSize() {
                Container parent = getParent();
                int maxWidth = 400;
                
                if (parent != null) {
                    maxWidth = Math.max(200, parent.getWidth() - 40);
                }
                
                setSize(maxWidth, Short.MAX_VALUE);
                Dimension size = super.getPreferredSize();
                
                return new Dimension(maxWidth, size.height);
            }
        };
        
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBorder(null);
        textPane.setFocusable(false);
        
        textPane.setContentType("text/html");
        textPane.setEditorKit(new HTMLEditorKit());
        
        String htmlContent = createFormattedHtmlContent(sender, userColor, textContent);
        textPane.setText(htmlContent);
        
        return textPane;
    }
    
    /**
     * Crea el contenido HTML formateado para el mensaje
     */
    private String createFormattedHtmlContent(String sender, String userColor, String textContent) {
        String escapedText = escapeHtml(textContent);
        
        return String.format(
            "<html>" +
            "<head><style>" +
            "body { font-family: Arial, sans-serif; margin: 0; padding: 0; }" +
            ".message { word-wrap: break-word; white-space: pre-wrap; overflow-wrap: break-word; display: block; }" +
            ".sender { font-weight: bold; color: %s; }" +
            "</style></head>" +
            "<body><div class='message'><span class='sender'>%s:</span> %s</div></body>" +
            "</html>",
            userColor,
            escapeHtml(sender),
            escapedText
        );
    }
    
    /**
     * Escapa caracteres especiales HTML para evitar problemas de renderizado
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}