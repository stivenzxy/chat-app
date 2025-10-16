package com.serverInfrastructure.adapters.commands;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.entities.User;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.observers.ActiveUserManager;

import java.util.List;
import java.util.Optional;

public class LoginCommandAdapter implements ProtocolCommandAdapter {

    private final LoginService loginService;
    private final ActiveUserManager activeUserManager = ActiveUserManager.getInstance();
    private CommandHandler commandHandler; // se inyecta después

    public LoginCommandAdapter(LoginService loginService) {
        this.loginService = loginService;
    }

    public void setCommandHandler(CommandHandler handler) {
        this.commandHandler = handler;
    }

    @Override
    public String getCommandName() {
        return "LOGIN";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() != 3)
            return parser.encode("ERROR", "Argumentos inválidos para LOGIN");

        LoginRequest request = new LoginRequest(parts.get(1), parts.get(2));
        Optional<User> userOptional = loginService.loginAndGetUser(request);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            connectionContext.setId(user.getId());
            activeUserManager.userLoggedIn(user.getUsername().value(), user);
            if (commandHandler != null && commandHandler.getServer() != null) {
                // Usamos reflexión mínima: exponemos método público en TcpServer para reenviar evento
                try {
                    commandHandler.getServer().getClass()
                            .getDeclaredMethod("fireClientIdentityUpdatedPublic", ClientConnection.class)
                            .invoke(commandHandler.getServer(), connectionContext);
                } catch (Exception ignored) {}
            }
            return parser.encode("OK", "Login exitoso");
        } else {
            return parser.encode("ERROR", "Credenciales inválidas");
        }
    }
}