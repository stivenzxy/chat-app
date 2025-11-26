package com.serverDomain.repositories;

import com.serverDomain.entities.Channel;
import com.serverDomain.valueObjects.ChannelMember;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository {
    Channel save(Channel channel);

    Optional<Channel> findById(Integer id);

    List<Channel> findAllForUser(String userId);

    void addMember(Integer channelId, String userId);

    boolean isMember(Integer channelId, String userId);

    List<String> findMemberUserIds(Integer channelId);

    List<String> findMemberUsernames(Integer channelId);

    List<Channel> findAll();

    void insertReplicated(Channel channel);

    List<ChannelMember> findAllMembers();
}
