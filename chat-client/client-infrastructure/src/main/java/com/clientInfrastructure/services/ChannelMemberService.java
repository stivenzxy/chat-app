package com.clientInfrastructure.services;

import com.clientApplication.commands.GetChannelMembersClientCommand;
import java.util.List;

public class ChannelMemberService {
    private final GetChannelMembersClientCommand getMembersCommand;

    public ChannelMemberService(GetChannelMembersClientCommand getMembersCommand) {
        this.getMembersCommand = getMembersCommand;
    }

    public List<String> getChannelMembers(String channelId) {
        return getMembersCommand.execute(channelId);
    }
}
