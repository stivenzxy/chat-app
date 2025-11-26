package com.serverInfrastructure.adapters.commands;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.useCases.LoginService;
import com.serverDomain.entities.User;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.services.CommandHandler;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;

import java.util.Base64;
import java.util.List;
import java.util.Optional;

public class LoginCommandAdapter implements ProtocolCommandAdapter {

    private final LoginService loginService;
    private final ActiveUserManager activeUserManager = ActiveUserManager.getInstance();
    private CommandHandler commandHandler;
    private ServerNetworkAdapter networkAdapter;

    public LoginCommandAdapter(LoginService loginService) {
        this.loginService = loginService;
    }

    public void setCommandHandler(CommandHandler handler) {
        this.commandHandler = handler;
    }

    public void setNetworkAdapter(ServerNetworkAdapter networkAdapter) {
        this.networkAdapter = networkAdapter;
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

            // Guardar el connectionId único ANTES de cualquier modificación
            String sessionConnectionId = connectionContext.getId();

            // IMPORTANTE: NO crear un nuevo User con connectionId
            // Mantener el userId ORIGINAL del usuario de la BD
            // El ActiveUserManager ahora debe mapear connectionId -> User (con userId
            // original)

            activeUserManager.userLoggedIn(user.getUsername().value(), user, sessionConnectionId);

            // Broadcast user status to peers
            if (networkAdapter != null) {
                networkAdapter.broadcastUserStatus(user.getUsername().value(), true);
            }

            // (El código para fireClientIdentityUpdated permanece igual)
            if (commandHandler != null && commandHandler.getServer() != null) {
                try {
                    commandHandler.getServer().getClass()
                            .getDeclaredMethod("fireClientIdentityUpdatedPublic", ClientConnection.class)
                            .invoke(commandHandler.getServer(), connectionContext);
                } catch (Exception ignored) {
                }
            }

            // --- INICIO DE LA MODIFICACIÓN ---
            String photoBase64 = "";
            if (user.getPhotoData() != null && user.getPhotoData().length > 0) {
                photoBase64 = Base64.getEncoder().encodeToString(user.getPhotoData());
            }

            // IMPORTANTE: Devolver el userId ORIGINAL (no el connectionId) para el cliente
            // El cliente necesita el userId para identificar al usuario en su base de datos
            // local
            return parser.encode("OK", "Login exitoso", user.getId(), user.getUsername().value(), photoBase64);
            // --- FIN DE LA MODIFICACIÓN ---

        } else {
            return parser.encode("ERROR", "Credenciales inválidas");
        }
    }
}