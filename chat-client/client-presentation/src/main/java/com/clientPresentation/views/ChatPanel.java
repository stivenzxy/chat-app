package com.clientPresentation.views;

import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.factories.CommandFactory;

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

    private JList<UserDTO> userList;
    private DefaultListModel<UserDTO> userListModel;
    private JTabbedPane chatTabs; // El nuevo panel de pestañas
    private Map<String, PrivateChatPanel> openChats;

    public ChatPanel(String selfUsername, CommandFactory commandFactory) {
        this.selfUsername = selfUsername;
        this.commandFactory = commandFactory;
        this.getUsersCommand = commandFactory.createGetUsersCommand();
        this.openChats = new HashMap<>();
        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de Usuarios (sin cambios)
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));
        userListScrollPane.setPreferredSize(new Dimension(200, 0));
        add(userListScrollPane, BorderLayout.WEST);

        // Panel de Chat (AHORA ES UN JTABBEDPANE)
        chatTabs = new JTabbedPane();
        add(chatTabs, BorderLayout.CENTER);

        // AÑADIR LISTENER PARA ABRIR CHATS
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 2) { // Doble clic
                    int index = userList.locationToIndex(evt.getPoint());
                    UserDTO selectedUser = userListModel.getElementAt(index);
                    openPrivateChat(selectedUser.getUsername());
                }
            }
        });
    }

    private void loadUsers() {
        // Ejecutar en un hilo separado para no bloquear la UI
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
        // Asegurarse de que se ejecute en el hilo de la UI
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
        // Si la pestaña ya está abierta, la seleccionamos
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
        // Asegurarnos de que la pestaña de chat existe
        if (!openChats.containsKey(sender)) {
            openPrivateChat(sender);
        }
        // Añadir el mensaje de audio al panel correcto
        PrivateChatPanel chatPanel = openChats.get(sender);
        chatPanel.appendAudioMessage(sender, audioData);
    }
}