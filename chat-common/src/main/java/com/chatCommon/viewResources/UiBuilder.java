package com.chatCommon.viewResources;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public final class UiBuilder {

    public static final Color FIELD_BACKGROUND = new Color(245, 245, 245);
    public static final Color BORDER_COLOR = new Color(200, 200, 200);
    public static final Color LABEL_COLOR = new Color(80, 80, 80);
    public static final Font FIELD_FONT = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font BUTTON_FONT = new Font("SansSerif", Font.BOLD, 14);
    public static final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 13);

    private UiBuilder() {}

    public static Border createRoundedBorder() {
        return BorderFactory.createCompoundBorder(
                new RoundBorder(10, BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 10, 5, 10) // Padding interno
        );
    }

    public static void styleField(JComponent field, Border border) {
        field.setBorder(border);
        field.setBackground(FIELD_BACKGROUND);
        field.setFont(FIELD_FONT);
        field.setPreferredSize(new Dimension(220, 38));
    }

    public static void styleButton(JButton button, Color backgroundColor) {
        button.setPreferredSize(new Dimension(220, 40));
        button.setFont(BUTTON_FONT);
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new RoundBorder(10, backgroundColor.darker()));
    }

    public static JPanel createLabelWithField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(LABEL_FONT);
        lbl.setForeground(LABEL_COLOR);
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }
}