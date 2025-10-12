package com.clientPresentation.views;

import com.clientApplication.dto.LoginRequest;
import com.clientApplication.dto.LoginResponse;
import com.clientApplication.useCases.LoginUseCase;
import javax.swing.*;
import java.awt.*;

/**
 * Representa la ventana de inicio de sesión.
 * Es responsable de capturar las credenciales del usuario y
 * comunicarse con la capa de aplicación (LoginUseCase).
 */
public class LoginView extends JFrame {

    private final LoginUseCase loginUseCase;

    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;

    public LoginView(LoginUseCase loginUseCase) {
        this.loginUseCase = loginUseCase;
        initComponents();
    }

    private void initComponents() {
        setTitle("Inicio de Sesión - Chat Universitario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null); // Centrar en la pantalla
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // --- Componentes ---
        usernameField = new JTextField(20);
        passwordField = new JPasswordField(20);
        loginButton = new JButton("Iniciar Sesión");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 12));

        // --- Layout ---
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2; // Ocupa 2 columnas
        add(new JLabel("Nombre de Usuario:"), gbc);

        gbc.gridy++;
        add(usernameField, gbc);

        gbc.gridy++;
        add(new JLabel("Contraseña:"), gbc);

        gbc.gridy++;
        add(passwordField, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        add(loginButton, gbc);

        // --- Action Listener ---
        loginButton.addActionListener(e -> onLogin());
    }

    private void onLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Usuario y contraseña no pueden estar vacíos.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 1. Crear el DTO de la petición
        LoginRequest request = new LoginRequest(username, password);

        // 2. Ejecutar el caso de uso
        LoginResponse response = loginUseCase.execute(request);

        // 3. Reaccionar a la respuesta del DTO
        if (response.isSuccess()) {
            JOptionPane.showMessageDialog(this, "¡Login exitoso! " + response.getMessage(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            // Aquí iría la lógica para abrir la ventana principal del chat.
            // Por ejemplo: new ChatView().setVisible(true);
            // this.dispose(); // Cierra la ventana de login
        } else {
            JOptionPane.showMessageDialog(this, response.getMessage(), "Fallo de Autenticación", JOptionPane.ERROR_MESSAGE);
        }
    }
}