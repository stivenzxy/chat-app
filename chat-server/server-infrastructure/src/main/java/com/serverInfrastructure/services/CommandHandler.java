package com.serverInfrastructure.services;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.observers.ActiveUserManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandHandler {
    private final ProtocolParser parser;
    private final Map<String, ProtocolCommandAdapter> commands = new HashMap<>();
    private TcpServer server;

    public CommandHandler(ProtocolParser parser) {
        this.parser = parser;
    }

    public void setServer(TcpServer server) {
        this.server = server;
    }

    public TcpServer getServer() {
        return this.server;
    }
    public void registerCommand(ProtocolCommandAdapter command) {
        commands.put(command.getCommandName().toUpperCase(), command);
    }

    public String process(List<String> parts, ClientConnection connection) { // Añadir parámetro
        if (parts.isEmpty()) return parser.encode("ERROR", "Comando vacío");

        String name = parts.getFirst().toUpperCase();
        
        // Manejar LOGOUT de manera especial
        if ("LOGOUT".equals(name)) {
            return handleLogout(connection);
        }
        
        ProtocolCommandAdapter command = commands.get(name);

        if (command == null) return parser.encode("ERROR", "Comando desconocido");

        try {
            return command.execute(parts, parser, connection);
        } catch (Exception e) {
            return parser.encode("ERROR", "Error interno: " + e.getMessage());
        }
    }
    
    private String handleLogout(ClientConnection connection) {
        try {
            String connectionId = connection.getId();
            System.out.println("DEBUG: Procesando LOGOUT para conexión: " + connectionId);
            
            // Notificar al ActiveUserManager que el usuario se ha desconectado
            ActiveUserManager.getInstance().userLoggedOut(connectionId);
            
            System.out.println("DEBUG: LOGOUT procesado exitosamente para: " + connectionId);
            return parser.encode("LOGOUT_SUCCESS", "Logout exitoso");
        } catch (Exception e) {
            System.out.println("DEBUG: Error durante LOGOUT: " + e.getMessage());
            e.printStackTrace();
            return parser.encode("ERROR", "Error durante logout: " + e.getMessage());
        }
    }
}