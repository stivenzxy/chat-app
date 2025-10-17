// Contenido del archivo RegisterUserPanel.java
package com.serverPresentation.views.components;

import com.chatCommon.viewResources.UiBuilder;
import com.serverApplication.dto.CreateUserRequest;
import com.serverPresentation.controllers.UserController;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter; // <<< AÑADIR IMPORT
import java.awt.*;
import java.io.File; // <<< AÑADIR IMPORT
import java.nio.file.Files; // <<< AÑADIR IMPORT

public class RegisterUserPanel extends JPanel {

    private final UserController userController;

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField ipField;

    // --- INICIO DE CAMBIOS EN CAMPOS ---
    private JButton selectPhotoButton;
    private JLabel photoFileNameLabel;
    private byte[] selectedPhotoBytes;
    // --- FIN DE CAMBIOS EN CAMPOS ---

    private JButton registerButton;

    public RegisterUserPanel(UserController userController) {
        this.userController = userController;
        initComponents();
    }

    private void initComponents() {
        // ... (configuración inicial del panel)
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

        // --- INICIO DE NUEVOS COMPONENTES PARA FOTO ---
        selectPhotoButton = new JButton("Seleccionar Foto...");
        photoFileNameLabel = new JLabel("Ningún archivo seleccionado.");
        photoFileNameLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));

        selectPhotoButton.addActionListener(e -> onSelectPhoto());
        // --- FIN DE NUEVOS COMPONENTES PARA FOTO ---

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(UiBuilder.createLabelWithField("Nombre de usuario", usernameField), gbc);
        gbc.gridx = 1;
        add(UiBuilder.createLabelWithField("Dirección IP", ipField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        add(UiBuilder.createLabelWithField("Email", emailField), gbc);

        // --- INICIO DE PANEL PARA FOTO ---
        gbc.gridx = 1;
        JPanel photoPanel = new JPanel(new BorderLayout(5,0));
        photoPanel.setOpaque(false);
        photoPanel.add(selectPhotoButton, BorderLayout.WEST);
        photoPanel.add(photoFileNameLabel, BorderLayout.CENTER);
        add(UiBuilder.createLabelWithField("Foto de Perfil", photoPanel), gbc);
        // --- FIN DE PANEL PARA FOTO ---

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

    private void onSelectPhoto() {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Imágenes (JPG, PNG, GIF)", "jpg", "jpeg", "png", "gif");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                // Limitar tamaño de archivo a 5MB
                if (selectedFile.length() > 5 * 1024 * 1024) {
                    JOptionPane.showMessageDialog(this, "El archivo es demasiado grande (máx 5MB).", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                this.selectedPhotoBytes = Files.readAllBytes(selectedFile.toPath());
                photoFileNameLabel.setText(selectedFile.getName());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al leer el archivo de imagen.", "Error", JOptionPane.ERROR_MESSAGE);
                this.selectedPhotoBytes = null;
                photoFileNameLabel.setText("Error al cargar.");
            }
        }
    }

    private void onRegisterUser() {
        String username = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String password = new String(passwordField.getPassword());
        String ip = ipField.getText().trim();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Los campos de texto son obligatorios.", "Error de validación", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            CreateUserRequest request = new CreateUserRequest(
                    username,
                    email,
                    password,
                    selectedPhotoBytes,
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
        ipField.setText("");
        selectedPhotoBytes = null;
        photoFileNameLabel.setText("Ningún archivo seleccionado.");
    }
}