package com.serverPresentation.view;

import com.serverApplication.mapper.UserMapper;
import com.serverApplication.useCase.CreateUserService;
import com.serverDomain.repository.UserRepository;
import com.serverDomain.service.PasswordHasher;
import com.serverInfrastructure.persistence.repository.MysqlUserRepository;
import com.serverInfrastructure.service.BcryptPasswordHasher;
import com.serverPresentation.controller.UserController;

import javax.swing.*;
import java.awt.*;

public class UserManagementView extends JFrame {

    private final UserController userController;

    private JTextField usernameField;
    private JTextField emailField;
    private JPasswordField passwordField;
    private JTextField ipField;
    private JTextField photoField;
    private JButton registerButton;

    public UserManagementView(UserController userController) {
        this.userController = userController;
        initComponents();
    }

    private void initComponents() {
        setTitle("Chat universitario - Servidor");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 550);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JMenuBar menuBar = new JMenuBar();
        JMenu optionsMenu = new JMenu("Opciones >");
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(230, 230, 230));

        JLabel titleLabel = new JLabel("Gestión de Usuarios");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Registrar", buildRegisterPanel());
        tabbedPane.addTab("Listado de usuarios", new JPanel()); // Placeholder
        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildRegisterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(235, 235, 235));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        usernameField = new JTextField(15);
        emailField = new JTextField(15);
        passwordField = new JPasswordField(15);
        ipField = new JTextField(15);
        photoField = new JTextField(15);

        panel.add(labelWithField("Nombre de usuario", usernameField), gbc);
        gbc.gridx = 1;
        panel.add(labelWithField("Dirección IP", ipField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(labelWithField("Email", emailField), gbc);
        gbc.gridx = 1;
        panel.add(labelWithField("Foto (URL o ruta)", photoField), gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(labelWithField("Contraseña", passwordField), gbc);

        gbc.gridx = 1;
        JPanel btnPanel = new JPanel();
        registerButton = new JButton("Registrar usuario");
        registerButton.setBackground(new Color(46, 153, 85));
        registerButton.setForeground(Color.WHITE);
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(200, 40));
        btnPanel.add(registerButton);
        panel.add(btnPanel, gbc);

        registerButton.addActionListener(e -> onRegisterUser());
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
            userController.onRegister(username, email, password, photoUrl, ip);
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

    private JPanel labelWithField(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(240, 240, 240));
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(field, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(250, 60));
        return panel;
    }

    // Este 'main' es tu Composition Root, ahora corregido
    public static void main(String[] args) {
        // --- Capa de Infraestructura ---
        PasswordHasher passwordHasher = new BcryptPasswordHasher();
        UserRepository userRepository = new MysqlUserRepository(); // Usando la que ya definimos

        // --- Capa de Aplicación ---
        UserMapper userMapper = new UserMapper(passwordHasher);
        CreateUserService createUserService = new CreateUserService(userRepository, userMapper);

        // --- Capa de Presentación ---
        UserController userController = new UserController(createUserService);
        UserManagementView userManagementView = new UserManagementView(userController);

        // --- INICIAR LA APLICACIÓN (Forma correcta para Swing) ---
        SwingUtilities.invokeLater(() -> {
            userManagementView.setVisible(true);
        });
    }
}