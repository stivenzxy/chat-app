package com.serverInfrastructure.adapters;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.network.ClientConnection; // Importar
import java.util.List;

public interface ProtocolCommandAdapter {
    String getCommandName();
    String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext);
}