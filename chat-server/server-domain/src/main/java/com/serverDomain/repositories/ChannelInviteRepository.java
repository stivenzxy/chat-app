package com.serverDomain.repositories;

import com.serverDomain.entities.ChannelInvite;

import java.util.List;

public interface ChannelInviteRepository {
    ChannelInvite save(ChannelInvite invite);
    void updateStatus(Integer inviteId, ChannelInvite.Status status);
    List<ChannelInvite> findPendingForUser(String userId);
}


