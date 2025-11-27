package com.serverInfrastructure.persistence.repository;

import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverInfrastructure.persistence.dao.ChannelInviteDAO;

import java.util.List;

public class ChannelInviteRepositoryImpl implements ChannelInviteRepository {
    private final ChannelInviteDAO channelInviteDAO = new ChannelInviteDAO();

    @Override
    public ChannelInvite save(ChannelInvite invite) {
        return channelInviteDAO.insert(invite);
    }

    @Override
    public void updateStatus(String inviteId, ChannelInvite.Status status) {
        channelInviteDAO.updateStatus(inviteId, status);
    }

    @Override
    public List<ChannelInvite> findPendingForUser(String userId) {
        return channelInviteDAO.findPendingForUser(userId);
    }

    @Override
    public List<ChannelInvite> findAll() {
        return channelInviteDAO.findAll();
    }

    @Override
    public void insertReplicated(ChannelInvite invite) {
        channelInviteDAO.insertReplicated(invite);
    }
}
