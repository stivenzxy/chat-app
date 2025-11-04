package com.clientPresentation.views;

import com.chatCommon.dto.ChannelInviteDTO;
import com.chatCommon.viewResources.UiBuilder;
import com.clientApplication.commands.ListPendingInvitesClientCommand;
import com.clientApplication.commands.RespondInviteClientCommand;
import com.clientApplication.factories.CommandFactory;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class InvitesPanel extends JPanel {
    private final ListPendingInvitesClientCommand listCmd;
    private final RespondInviteClientCommand respondCmd;
    private final DefaultListModel<ChannelInviteDTO> model = new DefaultListModel<>();
    private final JList<ChannelInviteDTO> list = new JList<>(model);
    private Runnable onAccepted;

    public InvitesPanel(CommandFactory factory, Runnable onAccepted) {
        this.listCmd = factory.createListPendingInvitesCommand();
        this.respondCmd = factory.createRespondInviteCommand();
        this.onAccepted = onAccepted;

        setLayout(new BorderLayout(10,10));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        list.setCellRenderer(new InviteRenderer());
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setFixedCellHeight(-1);
        list.setOpaque(false);

        add(new JScrollPane(list), BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(2, 1, 5, 5));
        actions.setOpaque(false);
        JButton accept = new JButton("Aceptar");
        JButton reject = new JButton("Rechazar");

        UiBuilder.styleButton(accept, new Color(46, 204, 113));  // Verde
        UiBuilder.styleButton(reject, new Color(231, 76, 60));   // Rojo

        actions.add(accept);
        actions.add(reject);
        add(actions, BorderLayout.SOUTH);


        accept.addActionListener(e -> respondSelected("ACCEPTED"));
        reject.addActionListener(e -> respondSelected("REJECTED"));

        refresh();
    }

    public void refresh() {
        new SwingWorker<List<ChannelInviteDTO>, Void>() {
            @Override protected List<ChannelInviteDTO> doInBackground() {
                return listCmd.execute(null);
            }
            @Override protected void done() {
                try {
                    model.clear();
                    for (var i : get()) model.addElement(i);
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private void respondSelected(String status) {
        ChannelInviteDTO inv = list.getSelectedValue();
        if (inv == null) return;
        
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { 
                return respondCmd.execute(new RespondInviteClientCommand.Request(
                        inv.getInviteId(), status, inv.getChannelId()
                ));
            }
            @Override protected void done() { 
                try { 
                    boolean ok = get(); 
                    if (ok) { 
                        refresh(); 
                        if ("ACCEPTED".equals(status) && onAccepted != null) {
                            onAccepted.run();
                        }
                    } 
                } catch (Exception ignored) {}
            }
        }.execute();
    }

    private static class InviteRenderer extends JPanel implements ListCellRenderer<ChannelInviteDTO> {
        private final JLabel lblIcon = new JLabel("📩");
        private final JLabel lblMessage = new JLabel();
        private final JLabel lblChannel = new JLabel();

        public InviteRenderer() {
            setLayout(new BorderLayout(10, 5));
            setOpaque(true);
            setBorder(UiBuilder.createRoundedBorder());
            lblMessage.setFont(UiBuilder.LABEL_FONT);
            lblMessage.setForeground(UiBuilder.LABEL_COLOR);
            lblChannel.setFont(UiBuilder.FIELD_FONT);
            lblChannel.setForeground(new Color(100, 100, 100));
        }

        @Override
        public Component getListCellRendererComponent(
                JList<? extends ChannelInviteDTO> list, ChannelInviteDTO inv, int index,
                boolean isSelected, boolean cellHasFocus) {

            String chanName = inv.getChannelName() != null ? inv.getChannelName() : "Canal";
            String inviterName = inv.getInviterUsername() != null ? inv.getInviterUsername() : inv.getInviterUserId();

            lblMessage.setText("<html><b>" + inviterName + "</b> te invitó a unirte a su canal</html>");
            lblChannel.setText("'" + chanName + "'");

            removeAll();
            add(lblIcon, BorderLayout.WEST);

            JPanel textPanel = new JPanel(new GridLayout(2,1,0,2));
            textPanel.setOpaque(false);
            textPanel.add(lblMessage);
            textPanel.add(lblChannel);
            add(textPanel, BorderLayout.CENTER);

            if (isSelected) {
                setBackground(new Color(220, 240, 255));
            } else {
                setBackground(UiBuilder.FIELD_BACKGROUND);
            }

            return this;
        }
    }
}


