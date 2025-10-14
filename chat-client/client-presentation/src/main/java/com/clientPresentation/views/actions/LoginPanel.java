package com.clientPresentation.views.actions;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.chatCommon.viewResources.UiBuilder;
import com.clientApplication.commands.contract.ClientCommand;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class LoginPanel extends JPanel {
    private final ClientCommand<LoginRequest, LoginResponse> loginCommand;
    private final Consumer<String> onLoginSuccessCallback;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginPanel(ClientCommand<LoginRequest, LoginResponse> loginCommand, Consumer<String> onLoginSuccessCallback) {
        this.loginCommand = loginCommand;
        this.onLoginSuccessCallback = onLoginSuccessCallback;
        initComponents();
    }

    private void initComponents() {
        setBackground(new Color(230, 230, 230));
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                BorderFactory.createEmptyBorder(30, 50, 30, 50)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Border roundedBorder = UiBuilder.createRoundedBorder();

        usernameField = new JTextField(15);
        UiBuilder.styleField(usernameField, roundedBorder);

        passwordField = new JPasswordField(15);
        UiBuilder.styleField(passwordField, roundedBorder);

        loginButton = new JButton("Iniciar sesión");
        UiBuilder.styleButton(loginButton, new Color(13, 149, 28));

        gbc.gridx = 0;
        gbc.gridwidth = 2;

        gbc.gridy = 0;
        add(UiBuilder.createLabelWithField("Nombre de usuario", usernameField), gbc);

        gbc.gridy++;
        add(UiBuilder.createLabelWithField("Contraseña", passwordField), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);

        loginButton.addActionListener(e -> onLogin());
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario y contraseña no pueden estar vacíos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoginRequest request = new LoginRequest(username, password);
        LoginResponse response = loginCommand.execute(request);

        if (response.isSuccess()) {
            if (onLoginSuccessCallback != null) {
                onLoginSuccessCallback.accept(username);
            }
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Fallo de Autenticación", JOptionPane.ERROR_MESSAGE);
        }
    }
}