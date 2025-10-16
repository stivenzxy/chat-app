package com.clientPresentation.views.components;

import com.chatCommon.dto.ChannelDTO;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * Panel for displaying the list of channels.
 * Handles channel selection and display.
 */
public class ChannelListPanel extends JPanel {
    private final DefaultListModel<ChannelDTO> channelsModel;
    private final JList<ChannelDTO> channelsList;

    public ChannelListPanel() {
        this.channelsModel = new DefaultListModel<>();
        this.channelsList = new JList<>(channelsModel);
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        
        channelsList.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ChannelDTO dto) {
                    setText(dto.getName());
                }
                return c;
            }
        });
        
        channelsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        add(new JScrollPane(channelsList), BorderLayout.CENTER);
    }

    public void addChannel(ChannelDTO channel) {
        channelsModel.addElement(channel);
    }

    public void removeChannel(ChannelDTO channel) {
        channelsModel.removeElement(channel);
    }

    public void clearChannels() {
        channelsModel.clear();
    }

    public ChannelDTO getSelectedChannel() {
        return channelsList.getSelectedValue();
    }

    public void addSelectionListener(ActionListener listener) {
        channelsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                listener.actionPerformed(null);
            }
        });
    }

    public void setSelectedChannel(ChannelDTO channel) {
        channelsList.setSelectedValue(channel, true);
    }
}
