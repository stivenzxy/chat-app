package com.clientPresentation.views;

import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.views.components.atoms.DisconnectButton;

import javax.swing.*;
import java.awt.*;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ChatPanel extends JPanel {

    private final ClientCommand<Void, GetUsersResponse> getUsersCommand;
    private final CommandFactory commandFactory;
    private final String selfUsername;
    private final Runnable onDisconnectCallback;
    private final com.clientApplication.handlers.MessageHandler messageHandler;

    private JList<UserDTO> userList;
    private DefaultListModel<UserDTO> userListModel;
    private JTabbedPane chatTabs;
    private ChannelsPanel channelsPanel;
    private JPanel invitesContainer;
    private InvitesPanel invitesPanel;
    private JButton invitesBtn;
    private boolean invitesVisible = false;
    private Map<String, PrivateChatPanel> openChats;

    public ChatPanel(String selfUsername, CommandFactory commandFactory, Runnable onDisconnectCallback, com.clientApplication.handlers.MessageHandler messageHandler) {
        this.selfUsername = selfUsername;
        this.commandFactory = commandFactory;
        this.onDisconnectCallback = onDisconnectCallback;
        this.messageHandler = messageHandler;
        this.getUsersCommand = commandFactory.createGetUsersCommand();
        this.openChats = new HashMap<>();
        initComponents();
        loadUsers();
    }

    private void showInvitesPanel() {
        if (invitesContainer == null) {
            invitesContainer = new JPanel(new BorderLayout());
            invitesPanel = new InvitesPanel(commandFactory, () -> {
                if (channelsPanel != null) {
                    channelsPanel.refreshChannels();
                    channelsPanel.refreshChannelMembers();
                }
            });
            invitesContainer.add(invitesPanel, BorderLayout.CENTER);
            invitesContainer.setPreferredSize(new Dimension(300, 0));
            add(invitesContainer, BorderLayout.EAST);
        }
        
        invitesVisible = !invitesVisible;
        
        if (invitesVisible) {
            if (invitesPanel != null) invitesPanel.refresh();
            invitesContainer.setVisible(true);
        } else {
            invitesContainer.setVisible(false);
        }
        
        invitesContainer.revalidate();
        invitesContainer.repaint();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Columna izquierda como Tabs: Usuarios / Canales
        JTabbedPane leftTabs = new JTabbedPane();
        JPanel usersTab = new JPanel(new BorderLayout(5,5));
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));
        usersTab.add(userListScrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        DisconnectButton disconnectButton = new DisconnectButton(onDisconnectCallback);
        buttonPanel.add(disconnectButton);
        usersTab.add(buttonPanel, BorderLayout.SOUTH);
        leftTabs.addTab("Usuarios", usersTab);

        leftTabs.setPreferredSize(new Dimension(240, 0));
        
        chatTabs = new JTabbedPane();
        
        channelsPanel = new ChannelsPanel(selfUsername, commandFactory, messageHandler, chatTabs);
        leftTabs.addTab("Canales", channelsPanel);
        
        add(leftTabs, BorderLayout.WEST);
        invitesBtn = new JButton("Invitaciones");
        invitesBtn.addActionListener(e -> showInvitesPanel());
        
        messageHandler.registerInviteListener(event -> SwingUtilities.invokeLater(() -> {
            if (invitesPanel != null && invitesVisible) {
                invitesPanel.refresh();
            }
        }));
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.add(invitesBtn);
        add(topBar, BorderLayout.NORTH);
        
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(chatTabs, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
        

        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    int index = userList.locationToIndex(evt.getPoint());
                    UserDTO selectedUser = userListModel.getElementAt(index);
                    openPrivateChat(selectedUser.getUsername());
                }
            }
        });
    }

    private void loadUsers() {
        new SwingWorker<GetUsersResponse, Void>() {
            @Override
            protected GetUsersResponse doInBackground() throws Exception {
                return getUsersCommand.execute(null);
            }

            @Override
            protected void done() {
                try {
                    GetUsersResponse response = get();
                    if (response.isSuccess()) {
                        userListModel.clear();
                        for (UserDTO user : response.getUsers()) {
                            userListModel.addElement(user);
                        }
                    } else {
                        JOptionPane.showMessageDialog(ChatPanel.this,
                                "Error al cargar usuarios: " + response.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(ChatPanel.this,
                            "Error fatal al cargar usuarios: " + e.getMessage(),
                            "Error de Conexión", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    public void addUserToList(UserDTO user) {
        SwingUtilities.invokeLater(() -> {
            userListModel.addElement(user);
        });
    }

    public void removeUserFromList(String userId) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < userListModel.getSize(); i++) {
                if (userListModel.getElementAt(i).getId().equals(userId)) {
                    userListModel.removeElementAt(i);
                    break;
                }
            }
        });
    }

    public void openPrivateChat(String username) {
        if (openChats.containsKey(username)) {
            chatTabs.setSelectedComponent(openChats.get(username));
        } else {
            PrivateChatPanel newChatPanel = new PrivateChatPanel(
                    selfUsername,
                    username,
                    commandFactory
            );
            openChats.put(username, newChatPanel);
            chatTabs.addTab(username, newChatPanel);
            chatTabs.setSelectedComponent(newChatPanel);
        }
    }

    public void receiveMessage(String sender, String content) {
        if (!openChats.containsKey(sender)) {
            openPrivateChat(sender);
        }
        PrivateChatPanel chatPanel = openChats.get(sender);

        MessageDTO message = new MessageDTO(sender, selfUsername, content);
        chatPanel.receiveMessage(message);
    }

    public void receiveAudioMessage(String sender, byte[] audioData) {
        if (!openChats.containsKey(sender)) {
            openPrivateChat(sender);
        }

        PrivateChatPanel chatPanel = openChats.get(sender);

        MessageDTO audioMessage = new MessageDTO(sender, selfUsername, audioData);
        chatPanel.receiveMessage(audioMessage);
    }
}