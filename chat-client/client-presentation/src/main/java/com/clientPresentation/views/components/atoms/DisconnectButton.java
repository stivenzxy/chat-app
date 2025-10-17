package com.clientPresentation.views.components.atoms;

import com.chatCommon.viewResources.UiBuilder;
import javax.swing.*;
import java.awt.*;

public class DisconnectButton extends JButton {

    public DisconnectButton(Runnable onDisconnect) {
        super("Desconectar");

        UiBuilder.styleButton(this, new Color(113, 5, 61));
        setPreferredSize(new Dimension(100, 30));
        setFont(new Font("SansSerif", Font.BOLD, 12));

        addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(
                    null,
                    "¿Estás seguro de que quieres desconectarte?",
                    "Confirmar Desconexión",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );

            if (confirm == JOptionPane.YES_OPTION) {
                onDisconnect.run();
            }
        });
    }
}