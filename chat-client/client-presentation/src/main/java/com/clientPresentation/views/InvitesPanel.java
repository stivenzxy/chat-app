package com.clientPresentation.views;

import com.chatCommon.dto.ChannelInviteDTO;
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
        setLayout(new BorderLayout(5,5));
        list.setCellRenderer(new DefaultListCellRenderer(){
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                Component c = super.getListCellRendererComponent(l, v, i, s, f);
                if (v instanceof ChannelInviteDTO inv) {
                    String chanName = inv.getChannelName() != null ? inv.getChannelName() : "Canal";
                    String inviterName = inv.getInviterUsername() != null ? inv.getInviterUsername() : inv.getInviterUserId();
                    setText(inviterName + ": Te invitó a unirte a su canal '" + chanName + "'");
                }
                return c;
            }
        });
        add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton accept = new JButton("Aceptar");
        JButton reject = new JButton("Rechazar");
        actions.add(reject); actions.add(accept);
        add(actions, BorderLayout.SOUTH);

        accept.addActionListener(e -> respondSelected("ACCEPTED"));
        reject.addActionListener(e -> respondSelected("REJECTED"));

        refresh();
    }

    public void refresh() {
        new SwingWorker<List<ChannelInviteDTO>, Void>() {
            @Override protected List<ChannelInviteDTO> doInBackground() { return listCmd.execute(null); }
            @Override protected void done() { try { model.clear(); for (var i : get()) model.addElement(i);} catch (Exception ignored) {} }
        }.execute();
    }

    private void respondSelected(String status) {
        ChannelInviteDTO inv = list.getSelectedValue();
        if (inv == null) return;
        
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() { 
                return respondCmd.execute(new RespondInviteClientCommand.Request(inv.getInviteId(), status, inv.getChannelId())); 
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
                } catch (Exception e) {
                } 
            }
        }.execute();
    }
}


