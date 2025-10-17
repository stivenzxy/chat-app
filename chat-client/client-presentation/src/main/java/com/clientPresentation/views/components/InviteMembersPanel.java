package com.clientPresentation.views.components;

import com.chatCommon.dto.ChannelDTO;
import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.InviteToChannelClientCommand;
import com.clientApplication.commands.contract.ClientCommand;

import javax.swing.*;
import java.awt.*;
import java.util.List;

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
        
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                int successCount = 0;
                for (UserDTO u : selectedUsers) {
                    try {
                        boolean result = inviteToChannelClientCommand.execute(
                            new InviteToChannelClientCommand.Request(selected.getId(), u.getUsername()));
                        if (result) successCount++;
                    } catch (Exception e) {
                        // Error inviting user
                    }
                }
                return successCount;
            }
            @Override protected void done() {
                try { 
                    int successCount = get(); 
                    JOptionPane.showMessageDialog(InviteMembersPanel.this, 
                        "Invitaciones enviadas: " + successCount + "/" + selectedUsers.size());
                    usersList.clearSelection();
                } catch (Exception e) { 
                    JOptionPane.showMessageDialog(InviteMembersPanel.this, 
                        "Error enviando invitaciones: " + e.getMessage(), 
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
