package com.serverPresentation.views.components;

import com.chatCommon.viewResources.UiBuilder;
import com.serverApplication.dto.UserPresentationDTO;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.views.models.UserTableModel;
import com.serverPresentation.observers.UserRegistrationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.List;

public class UserListPanel extends JPanel implements UserRegistrationObserver {

    private static final Logger logger = LoggerFactory.getLogger(UserListPanel.class);

    private final UserController userController;
    private final UserTableModel tableModel;
    private final JTable userTable;
    private final JLabel statusLabel;
    private final JTextField searchField;

    public UserListPanel(UserController userController) {
        this.userController = userController;
        this.tableModel = new UserTableModel();
        this.userTable = new JTable(tableModel);
        this.statusLabel = new JLabel("Cargando usuarios...");
        this.searchField = new JTextField(20);

        userController.addUserRegistrationObserver(this);

        initComponents();
        setupTable();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JPanel topPanel = createTopPanel();
        add(topPanel, BorderLayout.NORTH);

        JPanel centerPanel = createCenterPanel();
        add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = createBottomPanel();
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.setBackground(Color.WHITE);

        JLabel searchLabel = new JLabel("Buscar:");
        searchLabel.setFont(UiBuilder.LABEL_FONT);
        searchLabel.setForeground(UiBuilder.LABEL_COLOR);
        searchPanel.add(searchLabel);

        UiBuilder.styleField(searchField, UiBuilder.createRoundedBorder());
        searchField.setPreferredSize(new Dimension(200, 35));
        searchPanel.add(searchField);

        panel.add(searchPanel, BorderLayout.WEST);

        return panel;
    }

    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Lista de Usuarios"));
        panel.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setPreferredSize(new Dimension(0, 400));
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createBottomPanel() {
        // --- INICIO DE LA MODIFICACIÓN 1 ---
        // Se elimina el botón "Ver Foto" de este panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        panel.setBackground(Color.WHITE);

        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusLabel.setForeground(Color.GRAY);
        panel.add(statusLabel, BorderLayout.WEST);

        return panel;
        // --- FIN DE LA MODIFICACIÓN 1 ---
    }

    private void setupTable() {
        userTable.setRowHeight(30); // Aumentamos un poco la altura de la fila
        userTable.setGridColor(new Color(230, 230, 230));
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setSelectionBackground(new Color(220, 235, 255));
        userTable.setFont(new Font("SansSerif", Font.PLAIN, 12));

        userTable.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 12));
        userTable.getTableHeader().setBackground(new Color(240, 240, 240));
        userTable.getTableHeader().setForeground(Color.BLACK);

        userTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(150);

        // --- INICIO DE LA MODIFICACIÓN 2 ---
        // Añadimos el renderizador y editor para la columna del botón
        TableColumn actionColumn = userTable.getColumnModel().getColumn(5); // Columna "Acción"
        actionColumn.setCellRenderer(new ButtonRenderer());
        actionColumn.setCellEditor(new ButtonEditor(new JCheckBox()));
        actionColumn.setPreferredWidth(100);
        // --- FIN DE LA MODIFICACIÓN 2 ---


        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        userTable.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        userTable.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        TableRowSorter<UserTableModel> sorter = new TableRowSorter<>(tableModel);
        userTable.setRowSorter(sorter);

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterTable(); }
        });
    }

    private void filterTable() {
        String text = searchField.getText().trim();
        @SuppressWarnings("unchecked")
        TableRowSorter<UserTableModel> sorter =
                (TableRowSorter<UserTableModel>) userTable.getRowSorter();

        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
        }

        updateStatusLabel();
    }

    public void loadUsers() {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Cargando usuarios...");
            statusLabel.setForeground(Color.BLUE);
        });

        SwingWorker<List<UserPresentationDTO>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<UserPresentationDTO> doInBackground() {
                return userController.getAllUsers();
            }

            @Override
            protected void done() {
                try {
                    List<UserPresentationDTO> users = get();
                    tableModel.setUsers(users);
                    updateStatusLabel();
                    logger.info("Usuarios cargados exitosamente: {} usuarios", users.size());
                } catch (Exception e) {
                    logger.error("Error al cargar usuarios: {}", e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Error al cargar usuarios");
                        statusLabel.setForeground(Color.RED);
                        JOptionPane.showMessageDialog(
                                UserListPanel.this,
                                "Error al cargar los usuarios: " + e.getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    });
                }
            }
        };

        worker.execute();
    }

    private void updateStatusLabel() {
        SwingUtilities.invokeLater(() -> {
            int totalUsers = tableModel.getRowCount();
            int visibleUsers = userTable.getRowCount();

            if (totalUsers == visibleUsers) {
                statusLabel.setText(String.format("Total: %d usuarios", totalUsers));
            } else {
                statusLabel.setText(String.format("Mostrando: %d de %d usuarios", visibleUsers, totalUsers));
            }
            statusLabel.setForeground(Color.GRAY);
        });
    }

    @Override
    public void onUserRegistered(UserPresentationDTO user) {
        SwingUtilities.invokeLater(this::loadUsers);
    }

    @Override
    public void onUserRegistrationError(String error) {
        SwingUtilities.invokeLater(() -> {
            logger.error("Error en registro de usuario: {}", error);
            statusLabel.setText("Error en el registro: " + error);
            statusLabel.setForeground(Color.RED);
        });
    }

    @Override
    public void onUserListUpdated() {
        SwingUtilities.invokeLater(this::loadUsers);
    }

    // --- INICIO DE NUEVAS CLASES INTERNAS ---

    /**
     * Clase para renderizar (dibujar) un JButton en una celda de la tabla.
     */
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    /**
     * Clase para manejar los eventos de clic del botón en la celda.
     */
    class ButtonEditor extends DefaultCellEditor {
        protected JButton button;
        private String label;
        private boolean isPushed;
        private int selectedRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.selectedRow = row;
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            if (isPushed) {
                // Acción que se ejecuta al presionar el botón
                int modelRow = userTable.convertRowIndexToModel(selectedRow);
                UserPresentationDTO user = tableModel.getUserAt(modelRow);

                if (user != null) {
                    if (user.getPhotoData() != null && user.getPhotoData().length > 0) {
                        ImageIcon icon = new ImageIcon(user.getPhotoData());
                        Image image = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                        JLabel imageLabel = new JLabel(new ImageIcon(image));
                        JOptionPane.showMessageDialog(null, imageLabel, "Foto de " + user.getUsername(), JOptionPane.PLAIN_MESSAGE);

                    } else {
                        JOptionPane.showMessageDialog(null, "El usuario no tiene una foto de perfil.", "Información", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
            isPushed = false;
            return label;
        }

        @Override
        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }
    }
}