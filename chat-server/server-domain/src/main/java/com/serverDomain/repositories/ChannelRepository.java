package com.serverDomain.repositories;

import com.serverDomain.entities.Channel;
import com.serverDomain.valueObjects.ChannelMember;

import java.util.List;
import java.util.Optional;

public interface ChannelRepository {
    Channel save(Channel channel);

    Optional<Channel> findById(String id);

    List<Channel> findAllForUser(String userId);

    void addMember(String channelId, String userId);

    boolean isMember(String channelId, String userId);

    List<String> findMemberUserIds(String channelId);

    List<String> findMemberUsernames(String channelId);

    List<String> findMemberUsersWithNames(String channelId);

    List<Channel> findAll();

    void insertReplicated(Channel channel);

    List<ChannelMember> findAllMembers();
}
