package com.clientPresentation.views;

import com.chatCommon.dto.ChannelDTO;
import com.clientApplication.commands.CreateChannelClientCommand;
import com.clientApplication.commands.InviteToChannelClientCommand;
import com.clientApplication.commands.ListChannelsClientCommand;
import com.clientApplication.commands.SendChannelAudioClientCommand;
import com.clientApplication.commands.SendChannelMessageClientCommand;
import com.clientApplication.commands.GetChannelMembersClientCommand;
import com.clientApplication.factories.CommandFactory;
import com.clientApplication.commands.contract.ClientCommand;
import com.chatCommon.dto.GetUsersResponse;
import com.clientApplication.handlers.MessageHandler;
import com.clientApplication.listeners.ChannelAudioListener;
import com.clientApplication.listeners.ChannelMessageListener;
import com.clientApplication.events.ChannelAudioEvent;
import com.clientApplication.events.ChannelMessageEvent;
import com.clientPresentation.views.components.ChannelListPanel;
import com.clientPresentation.views.components.ChannelChatPanel;
import com.clientPresentation.views.components.CreateChannelPanel;
import com.clientPresentation.views.components.InviteMembersPanel;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class ChannelsPanel extends JPanel implements ChannelMessageListener, ChannelAudioListener {
    private final String selfUsername;
    private final CommandFactory commandFactory;
    private final MessageHandler messageHandler;

    private final ListChannelsClientCommand listChannelsCommand;
    private final SendChannelMessageClientCommand sendChannelMessageCommand;
    private final SendChannelAudioClientCommand sendChannelAudioCommand;
    private final InviteToChannelClientCommand inviteToChannelClientCommand;
    private final ClientCommand<Void, GetUsersResponse> getUsersCommand;

    private final ChannelListPanel channelListPanel;
    private final JTabbedPane mainChatTabs;
    private final java.util.Map<Integer, ChannelChatPanel> channelTabsById = new java.util.HashMap<>();
    private InviteMembersPanel invitePanel;

    public ChannelsPanel(String selfUsername, CommandFactory commandFactory, MessageHandler messageHandler, JTabbedPane mainChatTabs) {
        this.selfUsername = selfUsername;
        this.commandFactory = commandFactory;
        this.messageHandler = messageHandler;
        this.mainChatTabs = mainChatTabs;
        this.channelListPanel = new ChannelListPanel();
        this.listChannelsCommand = commandFactory.createListChannelsCommand();
        this.sendChannelMessageCommand = commandFactory.createSendChannelMessageCommand();
        this.sendChannelAudioCommand = commandFactory.createSendChannelAudioCommand();
        this.inviteToChannelClientCommand = commandFactory.createInviteToChannelCommand();
        this.getUsersCommand = commandFactory.createGetUsersCommand();

        setLayout(new BorderLayout(10, 10));
        initComponents();
        registerListeners();
        loadChannels();
    }

    private void initComponents() {
        setLayout(new BorderLayout(5,5));
        
        // Panel izquierdo con lista de canales y formularios
        JPanel leftPanel = new JPanel(new BorderLayout(5,5));
        leftPanel.add(channelListPanel, BorderLayout.CENTER);
        
        JPanel bottomForms = new JPanel(); 
        bottomForms.setLayout(new BoxLayout(bottomForms, BoxLayout.Y_AXIS));
        
        // Panel de invitaciones
        invitePanel = new InviteMembersPanel(selfUsername, inviteToChannelClientCommand, getUsersCommand);
        invitePanel.setVisible(false);
        bottomForms.add(invitePanel);
        
        bottomForms.add(Box.createVerticalStrut(8));
        
        // Panel de creación de canales
        CreateChannelPanel createPanel = new CreateChannelPanel(
            commandFactory.createCreateChannelCommand(), 
            this::loadChannels
        );
        bottomForms.add(createPanel);
        
        leftPanel.add(bottomForms, BorderLayout.SOUTH);
        leftPanel.setPreferredSize(new Dimension(240, 0));
        add(leftPanel, BorderLayout.CENTER);

        // Configurar listener de selección de canales
        channelListPanel.addSelectionListener(e -> {
            ChannelDTO selectedChannel = channelListPanel.getSelectedChannel();
            if (invitePanel != null) {
                invitePanel.setVisible(selectedChannel != null);
                invitePanel.setSelectedChannel(selectedChannel);
                if (selectedChannel != null) {
                    invitePanel.refreshUsers();
                }
                invitePanel.revalidate(); 
                invitePanel.repaint();
            }
            if (selectedChannel != null) {
                openChannelTab(selectedChannel);
            }
        });
    }


    private void registerListeners() {
        messageHandler.registerChannelMessageListener(this);
        messageHandler.registerChannelAudioListener(this);
    }

    private void loadChannels() {
        new SwingWorker<List<ChannelDTO>, Void>() {
            @Override protected List<ChannelDTO> doInBackground() { 
                return listChannelsCommand.execute(null); 
            }
            @Override protected void done() {
                try {
                    List<ChannelDTO> channels = get();
                    channelListPanel.clearChannels();
                    for (ChannelDTO c : channels) {
                        channelListPanel.addChannel(c);
                    }
                } catch (Exception e) {
                    // Error loading channels
                }
            }
        }.execute();
    }

    public void refreshChannels() { 
        loadChannels(); 
    }
    
    public void refreshChannelMembers() {
        for (ChannelChatPanel panel : channelTabsById.values()) {
            panel.refreshMembers();
        }
    }

    private void openChannelTab(ChannelDTO channel) {
        String tabTitle = "#" + channel.getName();
        if (channelTabsById.containsKey(channel.getId())) {
            ChannelChatPanel panel = channelTabsById.get(channel.getId());
            mainChatTabs.setSelectedComponent(panel);
            return;
        }
        ChannelChatPanel panel = new ChannelChatPanel(selfUsername, channel, sendChannelMessageCommand, sendChannelAudioCommand, commandFactory.createGetChannelMembersCommand(), commandFactory.createTranscribeAudioCommand());
        channelTabsById.put(channel.getId(), panel);
        mainChatTabs.addTab(tabTitle, panel);
        mainChatTabs.setSelectedComponent(panel);
    }

    @Override
    public void onChannelMessage(ChannelMessageEvent event) {
        if ("SYSTEM".equals(event.getSender()) && "MEMBERS_UPDATED".equals(event.getContent())) {
            SwingUtilities.invokeLater(() -> {
                ChannelChatPanel panel = channelTabsById.get(event.getChannelId());
                if (panel != null) {
                    panel.refreshMembers();
                }
            });
        } else {
            SwingUtilities.invokeLater(() -> dispatchToChannelTab(event.getChannelId(), event.getSender(), event.getContent(), null));
        }
    }

    @Override
    public void onChannelAudio(ChannelAudioEvent event) {
        SwingUtilities.invokeLater(() -> dispatchToChannelTab(event.getChannelId(), event.getSender(), null, event.getAudioBase64()));
    }

    private void dispatchToChannelTab(int channelId, String sender, String text, String audioBase64) {
        ChannelChatPanel panel = channelTabsById.get(channelId);
        if (panel != null) {
            if (text != null) panel.receiveText(sender, text);
            if (audioBase64 != null) panel.receiveAudio(sender, audioBase64);
        }
    }
}
