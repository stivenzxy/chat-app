package com.serverInfrastructure.adapters.commands;

import com.chatCommon.dto.connection.ConnectionRequest;
import com.chatCommon.dto.connection.ConnectionResponse;
import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.ConnectionCommand;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;

import java.util.List;

public class ConnectionCommandAdapter implements ProtocolCommandAdapter {
    private final ConnectionCommand command;

    public ConnectionCommandAdapter(ConnectionCommand command) {
        this.command = command;
    }

    @Override
    public String getCommandName() {
        return "CONNECTION";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser) {
        if (parts.size() < 3)
            return parser.encode("ERROR", "Faltan parámetros para CONNECTION");

        String ip = parts.get(1);
        int port = Integer.parseInt(parts.get(2));

        ConnectionRequest request = new ConnectionRequest(ip, port);
        ConnectionResponse response = command.execute(request);

        return parser.encode("CONNECTION_RESPONSE",
                String.valueOf(response.success()),
                response.message());
    }
}
