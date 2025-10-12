package com.clientPresentation.views;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.LoginClientCommand;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.views.actions.LoginPanel;

import javax.swing.*;
import java.awt.*;

public class MainClientView extends JFrame {

    public MainClientView(CommandFactory commandFactory) {
        initComponents(commandFactory);
    }

    private void initComponents(CommandFactory commandFactory) {
        setTitle("Chat Universitario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new  Color(230, 230, 230));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 0, 5, 0);

        // --- Encabezado ---
        JLabel titleLabel = new JLabel("¡Bienvenido al Chat Universitario!");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 32));
        titleLabel.setForeground(new Color(55, 65, 81));
        add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Para iniciar debe loguearse");
        subtitleLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitleLabel.setForeground(new Color(107, 114, 128));
        gbc.insets = new Insets(5, 0, 40, 0);
        add(subtitleLabel, gbc);

        // --- Panel de Acción (Login) ---
        // 1. Pedimos a la fábrica el comando que necesitamos.
        ClientCommand<LoginRequest, LoginResponse> loginCommand = commandFactory.createLoginCommand();

        // 2. Le pasamos el comando al panel que lo va a usar.
        LoginPanel loginPanel = new LoginPanel(loginCommand);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 5, 0);
        add(loginPanel, gbc);
    }
}