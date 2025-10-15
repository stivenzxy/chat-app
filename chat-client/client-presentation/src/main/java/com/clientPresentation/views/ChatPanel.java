package com.clientPresentation.views;

import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.components.DisconnectButton;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;

public class ChatPanel extends JPanel {

    private final ClientCommand<Void, GetUsersResponse> getUsersCommand;
    private final CommandFactory commandFactory;
    private final String selfUsername;
    private final Runnable onDisconnectCallback;

    private JList<UserDTO> userList;
    private DefaultListModel<UserDTO> userListModel;
    private JTabbedPane chatTabs;
    private Map<String, PrivateChatPanel> openChats;

    public ChatPanel(String selfUsername, CommandFactory commandFactory, Runnable onDisconnectCallback) {
        this.selfUsername = selfUsername;
        this.commandFactory = commandFactory;
        this.onDisconnectCallback = onDisconnectCallback;
        this.getUsersCommand = commandFactory.createGetUsersCommand();
        this.openChats = new HashMap<>();
        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));
        leftPanel.add(userListScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        DisconnectButton disconnectButton = new DisconnectButton(onDisconnectCallback);
        buttonPanel.add(disconnectButton);
        leftPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        leftPanel.setPreferredSize(new Dimension(200, 0));
        add(leftPanel, BorderLayout.WEST);

        chatTabs = new JTabbedPane();
        add(chatTabs, BorderLayout.CENTER);

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
        chatPanel.receiveMessage(sender, content);
    }

    public void receiveAudioMessage(String sender, byte[] audioData) {
        if (!openChats.containsKey(sender)) {
            openPrivateChat(sender);
        }

        PrivateChatPanel chatPanel = openChats.get(sender);
        chatPanel.appendAudioMessage(sender, audioData);
    }
}