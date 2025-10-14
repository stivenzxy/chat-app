package com.serverPresentation.views.actions;

import com.chatCommon.viewResources.UiBuilder;
import com.serverApplication.dto.CreateUserRequest;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;
import com.serverPresentation.controllers.UserController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class RegisterUserPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(RegisterUserPanel.class);

    private final UserController userController;

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField ipField;
    private JTextField photoField;
    private JButton registerButton;

    public RegisterUserPanel(UserController userController) {
        this.userController = userController;
        initComponents();
    }

    private void initComponents() {
        setBackground(new Color(245, 245, 245));
        setLayout(new GridBagLayout());

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createLineBorder(new Color(210, 210, 210))
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Border roundedBorder = UiBuilder.createRoundedBorder();

        usernameField = new JTextField(15);
        UiBuilder.styleField(usernameField, roundedBorder);

        emailField = new JTextField(15);
        UiBuilder.styleField(emailField, roundedBorder);

        passwordField = new JPasswordField(15);
        UiBuilder.styleField(passwordField, roundedBorder);

        ipField = new JTextField(15);
        UiBuilder.styleField(ipField, roundedBorder);

        photoField = new JTextField(15);
        UiBuilder.styleField(photoField, roundedBorder);

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(UiBuilder.createLabelWithField("Nombre de usuario", usernameField), gbc);
        gbc.gridx = 1;
        add(UiBuilder.createLabelWithField("Dirección IP", ipField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        add(UiBuilder.createLabelWithField("Email", emailField), gbc);
        gbc.gridx = 1;
        add(UiBuilder.createLabelWithField("Foto (URL o ruta)", photoField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        add(UiBuilder.createLabelWithField("Contraseña", passwordField), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        registerButton = new JButton("Registrar usuario");
        UiBuilder.styleButton(registerButton, new Color(46, 153, 85));
        add(registerButton, gbc);

        registerButton.addActionListener(e -> onRegisterUser());
    }

    private void onRegisterUser() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String photoUrl = photoField.getText().trim();
        String ip = ipField.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios.", "Error de validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            CreateUserRequest request = new CreateUserRequest(
                    new Username(username).toString(),
                    new Email(email).toString(),
                    password,
                    photoUrl,
                    ip
            );

            userController.onRegister(request);
            JOptionPane.showMessageDialog(this, "Usuario registrado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            clearFields();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al registrar usuario: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearFields() {
        usernameField.setText("");
        emailField.setText("");
        passwordField.setText("");
        photoField.setText("");
        ipField.setText("");
    }
}