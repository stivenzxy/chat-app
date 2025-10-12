package com.clientPresentation.views.actions;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.commands.contract.ClientCommand;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
public class LoginPanel extends JPanel {
    private final ClientCommand<LoginRequest, LoginResponse> loginCommand;

    private JTextField usernameField;
    private JTextField ipField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginPanel(ClientCommand<LoginRequest, LoginResponse> loginCommand) {
        this.loginCommand = loginCommand;
        initComponents();
    }

    private void initComponents() {
        // Estilos del panel principal tomados del RegisterUserPanel
        setBackground(new Color(230, 230, 230)); // Fondo del formulario como en el mockup
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(209, 213, 219), 1, true),
                BorderFactory.createEmptyBorder(30, 50, 30, 50)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Borde reutilizable para los campos de texto
        Border roundedBorder = BorderFactory.createCompoundBorder(
                new RoundBorder(10, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        );

        // --- Instanciación y estilizado de componentes ---
        usernameField = new JTextField(15);
        styleField(usernameField, roundedBorder);

        ipField = new JTextField(15);
        styleField(ipField, roundedBorder);

        passwordField = new JPasswordField(15);
        styleField(passwordField, roundedBorder);

        loginButton = new JButton("Iniciar sesión");
        styleButton(loginButton);

        // --- Layout del formulario 2x2 ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(createLabelWithField("Nombre de usuario", usernameField), gbc);

        gbc.gridx = 1;
        add(createLabelWithField("Dirección IP", ipField), gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        add(createLabelWithField("Contraseña", passwordField), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.SOUTH; // Centra el botón en su celda
        add(loginButton, gbc);

        loginButton.addActionListener(e -> onLogin());
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        // Nota: El campo IP no se usa en la lógica de login actual.

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario y contraseña no pueden estar vacíos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        LoginRequest request = new LoginRequest(username, password);
        LoginResponse response = loginCommand.execute(request);

        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, "¡Login exitoso!", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            // Lógica para cambiar al panel del chat
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Fallo de Autenticación", JOptionPane.ERROR_MESSAGE);
        }
    }

    // --- MÉTODOS DE ESTILIZADO REPLICADOS DE RegisterUserPanel ---

    private void styleField(JComponent field, Border border) {
        field.setBorder(border);
        field.setBackground(new Color(245, 245, 245));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setPreferredSize(new Dimension(220, 38)); // Tamaño ajustado
    }

    private void styleButton(JButton button) {
        button.setPreferredSize(new Dimension(220, 38));
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setBackground(new Color(12, 58, 137)); // Azul del mockup
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(new RoundBorder(10, button.getBackground().darker()));
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