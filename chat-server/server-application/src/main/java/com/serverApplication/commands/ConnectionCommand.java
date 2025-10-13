package com.serverApplication.commands;

import com.chatCommon.dto.connection.ConnectionRequest;
import com.chatCommon.dto.connection.ConnectionResponse;
import com.serverApplication.commands.contract.Command;

public class ConnectionCommand implements Command<ConnectionRequest, ConnectionResponse> {

    @Override
    public ConnectionResponse execute(ConnectionRequest request) {
        return new ConnectionResponse(true, "Conexión establecida correctamente");
    }
}
