package com.clientPresentation.views;

import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientPresentation.views.components.atoms.DisconnectButton;

import com.chatCommon.dto.UserDTO;
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
    private final UserDTO selfUser;

    private JList<UserDTO> userList;
    private DefaultListModel<UserDTO> userListModel;
    private JTabbedPane chatTabs;
    private ChannelsPanel channelsPanel;
    private JPanel invitesContainer;
    private InvitesPanel invitesPanel;
    private JButton invitesBtn;
    private boolean invitesVisible = false;
    private Map<String, PrivateChatPanel> openChats;

    public ChatPanel(UserDTO selfUser, CommandFactory commandFactory, Runnable onDisconnectCallback, com.clientApplication.handlers.MessageHandler messageHandler) {
        this.selfUser = selfUser;
        this.selfUsername = selfUser.getUsername(); // Conveniencia
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

        // --- INICIO DE MODIFICACIÓN DE LA ESTRUCTURA ---
        JPanel profilePanel = createProfilePanel();

        JTabbedPane leftTabs = new JTabbedPane();
        JPanel usersTab = new JPanel(new BorderLayout(5,5));
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setCellRenderer(new UserListRenderer());
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));
        usersTab.add(userListScrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        DisconnectButton disconnectButton = new DisconnectButton(onDisconnectCallback);
        buttonPanel.add(disconnectButton);
        usersTab.add(buttonPanel, BorderLayout.SOUTH);
        leftTabs.addTab("Usuarios", usersTab);

        chatTabs = new JTabbedPane();
        channelsPanel = new ChannelsPanel(selfUsername, commandFactory, messageHandler, chatTabs);
        leftTabs.addTab("Canales", channelsPanel);

        // Nuevo panel izquierdo que combina perfil y pestañas
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        leftPanel.add(profilePanel, BorderLayout.NORTH);
        leftPanel.add(leftTabs, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(240, 0));

        add(leftPanel, BorderLayout.WEST);
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

    private JPanel createProfilePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Mi Perfil"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        JLabel nameLabel = new JLabel(selfUser.getUsername());
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel photoLabel = new JLabel();
        photoLabel.setPreferredSize(new Dimension(64, 64));
        photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        photoLabel.setVerticalAlignment(SwingConstants.CENTER);

        byte[] photoData = selfUser.getPhotoData();
        if (photoData != null && photoData.length > 0) {
            ImageIcon icon = new ImageIcon(photoData);
            Image image = icon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
            photoLabel.setIcon(new ImageIcon(image));
        } else {
            photoLabel.setIcon(UIManager.getIcon("OptionPane.questionIcon"));
        }

        panel.add(photoLabel, BorderLayout.WEST);
        panel.add(nameLabel, BorderLayout.CENTER);

        return panel;
    }

    // --- INICIO DE NUEVA CLASE INTERNA ---
    class UserListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof UserDTO user) {
                setText(user.getUsername());
                byte[] photoData = user.getPhotoData();
                if (photoData != null && photoData.length > 0) {
                    ImageIcon icon = new ImageIcon(photoData);
                    Image image = icon.getImage().getScaledInstance(32, 32, Image.SCALE_SMOOTH);
                    setIcon(new ImageIcon(image));
                } else {
                    // Placeholder icon if no photo
                    setIcon(UIManager.getIcon("OptionPane.questionIcon"));
                }
                setIconTextGap(10);
            }
            return this;
        }
    }
    // --- FIN DE NUEVA CLASE INTERNA ---

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
                            // No agregar al propio usuario a la lista
                            if (!user.getUsername().equals(selfUsername)) {
                                userListModel.addElement(user);
                            }
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
            // No agregar al propio usuario
            if (user.getUsername().equals(selfUsername)) {
                return;
            }
            
            // Verificar si el usuario ya existe en la lista
            boolean userExists = false;
            for (int i = 0; i < userListModel.getSize(); i++) {
                UserDTO existingUser = userListModel.getElementAt(i);
                if (existingUser.getId().equals(user.getId()) || 
                    existingUser.getUsername().equals(user.getUsername())) {
                    userExists = true;
                    break;
                }
            }
            
            if (!userExists) {
                userListModel.addElement(user);
            }
        });
    }

    public void removeUserFromList(String username) {
        SwingUtilities.invokeLater(() -> {
            int userIndex = -1;

            for (int i = 0; i < userListModel.getSize(); i++) {
                if (userListModel.getElementAt(i).getUsername().equals(username)) {
                    userIndex = i;
                    break;
                }
            }

            if (userIndex != -1) {
                userListModel.removeElementAt(userIndex);
                closePrivateChatTab(username);
            }
        });
    }

    private void closePrivateChatTab(String username) {
        if (openChats.containsKey(username)) {
            Component chatComponent = openChats.get(username);
            int tabIndex = chatTabs.indexOfComponent(chatComponent);

            if (tabIndex != -1) {
                chatTabs.removeTabAt(tabIndex);
            }

            openChats.remove(username);

            JOptionPane.showMessageDialog(
                    this,
                    "El usuario '" + username + "' se ha desconectado. El chat se ha cerrado.",
                    "Usuario Desconectado",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
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
    
    public void receiveEchoMessage(String recipient, String content) {
        if (!openChats.containsKey(recipient)) {
            openPrivateChat(recipient);
        }
        PrivateChatPanel chatPanel = openChats.get(recipient);

        MessageDTO message = new MessageDTO(selfUsername, recipient, content);
        chatPanel.displayEchoMessage(message);
    }
    
    public void receiveEchoAudioMessage(String recipient, byte[] audioData) {
        if (!openChats.containsKey(recipient)) {
            openPrivateChat(recipient);
        }

        PrivateChatPanel chatPanel = openChats.get(recipient);

        MessageDTO audioMessage = new MessageDTO(selfUsername, recipient, audioData);
        chatPanel.displayEchoMessage(audioMessage);
    }
}