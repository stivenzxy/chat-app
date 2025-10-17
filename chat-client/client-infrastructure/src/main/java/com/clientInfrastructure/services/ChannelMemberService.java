package com.clientInfrastructure.services;

import com.clientApplication.commands.GetChannelMembersClientCommand;
import java.util.List;

/**
 * Service responsible for channel member operations.
 */
public class ChannelMemberService {
    private final GetChannelMembersClientCommand getMembersCommand;
    
    public ChannelMemberService(GetChannelMembersClientCommand getMembersCommand) {
        this.getMembersCommand = getMembersCommand;
    }
    
    /**
     * Retrieves channel members from the server.
     */
    public List<String> getChannelMembers(int channelId) {
        return getMembersCommand.execute(channelId);
    }
}
