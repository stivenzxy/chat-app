package com.clientPresentation.views.components;

import com.chatCommon.dto.ChannelDTO;
import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.InviteToChannelClientCommand;
import com.clientApplication.commands.contract.ClientCommand;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Panel for inviting members to channels.
 * Handles user selection and invitation sending.
 */
public class InviteMembersPanel extends JPanel {
    private final String selfUsername;
    private final InviteToChannelClientCommand inviteToChannelClientCommand;
    private final ClientCommand<Void, GetUsersResponse> getUsersCommand;

    private DefaultListModel<UserDTO> inviteUsersModel;
    private JList<UserDTO> usersList;

    public InviteMembersPanel(String selfUsername,
                              InviteToChannelClientCommand inviteToChannelClientCommand,
                              ClientCommand<Void, GetUsersResponse> getUsersCommand) {
        this.selfUsername = selfUsername;
        this.inviteToChannelClientCommand = inviteToChannelClientCommand;
        this.getUsersCommand = getUsersCommand;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5,5));
        setBorder(BorderFactory.createTitledBorder("Invitar miembros"));

        inviteUsersModel = new DefaultListModel<>();
        usersList = new JList<>(inviteUsersModel);
        usersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        usersList.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UserDTO u) {
                    setText(u.getUsername());
                }
                return c;
            }
        });

        JButton refreshBtn = new JButton("Actualizar usuarios");
        refreshBtn.addActionListener(a -> loadUsers());

        JButton inviteBtn = new JButton("Invitar seleccionados");
        inviteBtn.setBackground(new Color(0, 150, 0));
        inviteBtn.setForeground(Color.WHITE);
        inviteBtn.setOpaque(true);
        inviteBtn.setBorderPainted(false);
        inviteBtn.addActionListener(a -> inviteSelectedUsers());

        JPanel actions = new JPanel(new GridLayout(2, 1, 5, 5));
        actions.setBorder(BorderFactory.createTitledBorder("Acciones"));
        actions.add(refreshBtn);
        actions.add(inviteBtn);

        JScrollPane scrollPane = new JScrollPane(usersList);
        scrollPane.setPreferredSize(new Dimension(200, 120));
        scrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios conectados"));

        add(scrollPane, BorderLayout.CENTER);
        add(actions, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(220, 200));
        setPreferredSize(new Dimension(220, 200));
    }

    private void loadUsers() {
        new SwingWorker<GetUsersResponse, Void>() {
            @Override protected GetUsersResponse doInBackground() {
                return getUsersCommand.execute(null);
            }
            @Override protected void done() {
                try {
                    GetUsersResponse resp = get();
                    inviteUsersModel.clear();
                    if (resp != null && resp.isSuccess()) {
                        for (UserDTO u : resp.getUsers()) {
                            if (!u.getUsername().equals(selfUsername)) {
                                inviteUsersModel.addElement(u);
                            }
                        }
                    }
                } catch (Exception e) {
                    // Error loading users
                }
            }
        }.execute();
    }

    private void inviteSelectedUsers() {
        ChannelDTO selected = getSelectedChannel();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Selecciona un canal primero");
            return;
        }

        List<UserDTO> selectedUsers = usersList.getSelectedValuesList();
        if (selectedUsers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona al menos un usuario para invitar");
            return;
        }

        // Caso especial para un solo usuario para dar una retroalimentación más clara
        if (selectedUsers.size() == 1) {
            UserDTO singleUser = selectedUsers.getFirst();
            new SwingWorker<String, Void>() {
                @Override
                protected String doInBackground() {
                    return inviteToChannelClientCommand.execute(
                            new InviteToChannelClientCommand.Request(selected.getId(), singleUser.getUsername()));
                }

                @Override
                protected void done() {
                    try {
                        String result = get();
                        if ("OK".equals(result)) {
                            JOptionPane.showMessageDialog(InviteMembersPanel.this,
                                    "Invitación enviada correctamente a " + singleUser.getUsername() + ".",
                                    "Éxito",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else if (result != null && result.contains("ya es miembro")) {
                            JOptionPane.showMessageDialog(InviteMembersPanel.this,
                                    "Este usuario ya se encuentra en el canal.",
                                    "Aviso",
                                    JOptionPane.WARNING_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(InviteMembersPanel.this,
                                    "No se pudo enviar la invitación: " + result,
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                        usersList.clearSelection();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(InviteMembersPanel.this,
                                "Error crítico al enviar invitación: " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
            return; // Detener la ejecución para el caso de un solo usuario
        }

        // Lógica para múltiples usuarios
        new SwingWorker<Map<String, String>, Void>() {
            @Override
            protected Map<String, String> doInBackground() {
                Map<String, String> results = new LinkedHashMap<>();
                for (UserDTO u : selectedUsers) {
                    String result = inviteToChannelClientCommand.execute(
                            new InviteToChannelClientCommand.Request(selected.getId(), u.getUsername()));
                    results.put(u.getUsername(), result);
                }
                return results;
            }

            @Override
            protected void done() {
                try {
                    Map<String, String> results = get();
                    StringBuilder summary = new StringBuilder("<html><b>Resumen de Invitaciones:</b><br><hr>");

                    for (Map.Entry<String, String> entry : results.entrySet()) {
                        summary.append("<b>").append(entry.getKey()).append(":</b> ");
                        String result = entry.getValue();
                        if ("OK".equals(result)) {
                            summary.append("<font color='green'>Invitación enviada.</font><br>");
                        } else if (result != null && result.contains("ya es miembro")) {
                            summary.append("<font color='orange'>Ya es miembro.</font><br>");
                        } else {
                            summary.append("<font color='red'>Error (").append(result).append(")</font><br>");
                        }
                    }
                    summary.append("</html>");

                    JOptionPane.showMessageDialog(InviteMembersPanel.this, summary.toString(),
                            "Resultados", JOptionPane.INFORMATION_MESSAGE);

                    usersList.clearSelection();
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(InviteMembersPanel.this,
                            "Error crítico al enviar invitaciones: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private ChannelDTO selectedChannel;

    private ChannelDTO getSelectedChannel() {
        return selectedChannel;
    }

    public void setSelectedChannel(ChannelDTO channel) {
        this.selectedChannel = channel;
    }

    public void refreshUsers() {
        loadUsers();
    }
}