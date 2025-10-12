package com.serverInfrastructure.services;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.commands.LoginCommand;
import com.serverApplication.commands.contract.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandHandler {
    private final Map<String, Command<?, ?>> commandMap = new HashMap<>();
    private final ProtocolParser parser;

    public CommandHandler(ProtocolParser parser) {
        this.parser = parser;
    }

    public void registerCommand(String commandName, Command<?, ?> command) {
        commandMap.put(commandName.toUpperCase(), command);
    }

    public String process(List<String> parts) {
        if (parts.isEmpty() || parts.getFirst().isEmpty()) {
            return parser.encode("ERROR", "Comando inválido");
        }

        String commandName = parts.getFirst().toUpperCase();
        Command<?, ?> command = commandMap.get(commandName);

        if (command == null) {
            return parser.encode("ERROR", "Comando desconocido");
        }

        // --- ZONA DE TRADUCCIÓN ---
        // Aquí es donde la infraestructura conoce los detalles de cada comando
        try {
            if (command instanceof LoginCommand) {
                // 1. Traducir de List<String> a RequestDTO
                if (parts.size() != 3) return parser.encode("ERROR", "Argumentos inválidos para LOGIN");
                LoginRequest requestDTO = new LoginRequest(parts.get(1), parts.get(2));

                // 2. Ejecutar el comando de aplicación
                LoginResponse responseDTO = ((LoginCommand) command).execute(requestDTO);

                // 3. Traducir de ResponseDTO a String del protocolo
                String status = responseDTO.isSuccess() ? "OK" : "ERROR";
                return parser.encode(status, responseDTO.getMessage());
            }
            // else if (command instanceof SendMessageCommand) { ... }

            return parser.encode("ERROR", "Handler no implementado para este comando");
        } catch (Exception e) {
            return parser.encode("ERROR", "Error interno procesando el comando: " + e.getMessage());
        }
    }
}