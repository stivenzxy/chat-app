package com.serverInfrastructure.network.protocol;

import com.chatCommon.protocol.ProtocolParser;

import java.util.List;

public interface ProtocolCommandAdapter {
    String getCommandName();
    String execute(List<String> parts, ProtocolParser parser);
}
