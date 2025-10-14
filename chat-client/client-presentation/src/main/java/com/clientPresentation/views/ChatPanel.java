package com.clientPresentation.views;

import com.chatCommon.dto.GetUsersResponse;
import com.chatCommon.dto.UserDTO;
import com.clientApplication.commands.contract.ClientCommand;

import javax.swing.*;
import java.awt.*;
import java.util.Vector;

public class ChatPanel extends JPanel {

    private final ClientCommand<Void, GetUsersResponse> getUsersCommand;
    private JList<UserDTO> userList;
    private DefaultListModel<UserDTO> userListModel;

    public ChatPanel(ClientCommand<Void, GetUsersResponse> getUsersCommand) {
        this.getUsersCommand = getUsersCommand;
        initComponents();
        loadUsers();
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel de Usuarios a la izquierda
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        JScrollPane userListScrollPane = new JScrollPane(userList);
        userListScrollPane.setBorder(BorderFactory.createTitledBorder("Usuarios Conectados"));
        userListScrollPane.setPreferredSize(new Dimension(200, 0));
        add(userListScrollPane, BorderLayout.WEST);

        // Panel de Chat (placeholder por ahora)
        JPanel chatArea = new JPanel();
        chatArea.setBorder(BorderFactory.createTitledBorder("Chat"));
        add(chatArea, BorderLayout.CENTER);
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
}