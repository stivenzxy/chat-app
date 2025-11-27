package com.clientPresentation.views.components;

import com.chatCommon.dto.ChannelVisibility;
import com.clientApplication.commands.CreateChannelClientCommand;

import javax.swing.*;
import java.awt.*;

public class CreateChannelPanel extends JPanel {
    private final CreateChannelClientCommand createChannelCommand;
    private final Runnable onChannelCreated;

    public CreateChannelPanel(CreateChannelClientCommand createChannelCommand, Runnable onChannelCreated) {
        this.createChannelCommand = createChannelCommand;
        this.onChannelCreated = onChannelCreated;
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridLayout(0, 1, 5, 5));
        setBorder(BorderFactory.createTitledBorder("Nuevo canal"));

        JTextField nameField = new JTextField();
        JComboBox<ChannelVisibility> visibility = new JComboBox<>(ChannelVisibility.values());
        visibility.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChannelVisibility v) {
                    setText(v == ChannelVisibility.PUBLIC ? "Público" : "Privado");
                }
                return c;
            }
        });

        JButton createBtn = new JButton("Crear canal");
        createBtn.addActionListener(a -> {
            String channelName = nameField.getText().trim();
            if (channelName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El nombre del canal no puede estar vacío", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return createChannelCommand.execute(new CreateChannelClientCommand.Request(channelName,
                            (ChannelVisibility) visibility.getSelectedItem()));
                }

                @Override
                protected void done() {
                    try {
                        String result = get();
                        if (result != null && !result.isEmpty()) {
                            nameField.setText("");
                            onChannelCreated.run();
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(CreateChannelPanel.this,
                                "Error al crear canal: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });

        add(new JLabel("Nombre"));
        add(nameField);
        add(new JLabel("Visibilidad"));
        add(visibility);
        add(createBtn);
    }
}
