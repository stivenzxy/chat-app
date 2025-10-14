package com.serverInfrastructure.services;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandHandler {
    private final ProtocolParser parser;
    private final Map<String, ProtocolCommandAdapter> commands = new HashMap<>();

    public CommandHandler(ProtocolParser parser) {
        this.parser = parser;
    }

    public void registerCommand(ProtocolCommandAdapter command) {
        commands.put(command.getCommandName().toUpperCase(), command);
    }

    public String process(List<String> parts) {
        if (parts.isEmpty()) return parser.encode("ERROR", "Comando vacío");

        String name = parts.getFirst().toUpperCase();
        ProtocolCommandAdapter command = commands.get(name);

        if (command == null) return parser.encode("ERROR", "Comando desconocido");

        try {
            return command.execute(parts, parser);
        } catch (Exception e) {
            return parser.encode("ERROR", "Error interno: " + e.getMessage());
        }
    }
}