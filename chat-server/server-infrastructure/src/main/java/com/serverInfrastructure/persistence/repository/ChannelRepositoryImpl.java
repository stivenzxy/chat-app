package com.serverInfrastructure.persistence.repository;

import com.serverDomain.entities.Channel;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.persistence.dao.ChannelDAO;

import java.util.List;
import java.util.Optional;

public class ChannelRepositoryImpl implements ChannelRepository {
    private final ChannelDAO channelDAO = new ChannelDAO();

    @Override
    public Channel save(Channel channel) {
        return channelDAO.insert(channel);
    }

    @Override
    public Optional<Channel> findById(Integer id) {
        return channelDAO.findById(id);
    }

    @Override
    public List<Channel> findAllForUser(String userId) {
        return channelDAO.findAllForUser(userId);
    }

    @Override
    public void addMember(Integer channelId, String userId) {
        channelDAO.addMember(channelId, userId);
    }

    @Override
    public boolean isMember(Integer channelId, String userId) {
        return channelDAO.isMember(channelId, userId);
    }

    @Override
    public List<String> findMemberUserIds(Integer channelId) {
        return channelDAO.findMemberUserIds(channelId);
    }

    @Override
    public List<String> findMemberUsernames(Integer channelId) {
        return channelDAO.findMemberUsernames(channelId);
    }
}


