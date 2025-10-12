package com.serverPresentation.views.actions;

import com.serverApplication.dto.CreateUserRequest;
import com.serverDomain.valueObjects.Email;
import com.serverDomain.valueObjects.Username;
import com.serverPresentation.controllers.UserController;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class RegisterUserPanel extends JPanel {

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
        setBackground(new Color(235, 235, 235));
        setLayout(new GridBagLayout());

        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createLineBorder(new Color(210, 210, 210))
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Border roundedBorder = BorderFactory.createCompoundBorder(
                new RoundBorder(10, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        );

        usernameField = new JTextField(15);
        styleField(usernameField, roundedBorder);

        emailField = new JTextField(15);
        styleField(emailField, roundedBorder);

        passwordField = new JPasswordField(15);
        styleField(passwordField, roundedBorder);

        ipField = new JTextField(15);
        styleField(ipField, roundedBorder);

        photoField = new JTextField(15);
        styleField(photoField, roundedBorder);
        
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(createLabelWithField("Nombre de usuario", usernameField), gbc);
        gbc.gridx = 1;
        add(createLabelWithField("Dirección IP", ipField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        add(createLabelWithField("Email", emailField), gbc);
        gbc.gridx = 1;
        add(createLabelWithField("Foto (URL o ruta)", photoField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        add(createLabelWithField("Contraseña", passwordField), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.SOUTH;
        registerButton = new JButton("Registrar usuario");
        registerButton.setBackground(new Color(46, 153, 85));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(200, 40));
        registerButton.setFocusPainted(false);
        registerButton.setBorder(new RoundBorder(10, registerButton.getBackground().darker()));
        add(registerButton, gbc);

        registerButton.addActionListener(e -> onRegisterUser());
    }

    private void styleField(JComponent field, Border border) {
        field.setBorder(border);
        field.setBackground(new Color(245, 245, 245));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    private JPanel createLabelWithField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(0, 5));
        panel.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 13));
        lbl.setForeground(new Color(80, 80, 80));
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        return panel;
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

    private record RoundBorder(int radius, Color color) implements Border {

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(this.radius / 2, this.radius / 2, this.radius / 2, this.radius / 2);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }
}