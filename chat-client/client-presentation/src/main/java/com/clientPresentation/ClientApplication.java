package com.clientPresentation;

import com.clientApplication.ports.AuthPort;
import com.clientApplication.useCases.LoginUseCase;
import com.clientInfrastructure.adapters.TcpAuthAdapter;
import com.clientInfrastructure.network.TcpClient;
import com.clientPresentation.views.LoginView;
import com.protocol.ProtocolParser;

import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        // --- Configuración del Cliente ---
        final String SERVER_HOST = "localhost";
        final int SERVER_PORT = 12345; // Debe coincidir con el puerto del servidor

        // --- Creación y Ensamblaje de Componentes (Inyección de Dependencias) ---

        // 1. Crear componentes de Infraestructura (los detalles de bajo nivel)
        ProtocolParser parser = new ProtocolParser('|', '\\');
        TcpClient tcpClient = new TcpClient(SERVER_HOST, SERVER_PORT);

        // 2. Crear el Adaptador (el puente entre Infraestructura y Aplicación)
        AuthPort authAdapter = new TcpAuthAdapter(tcpClient, parser);

        // 3. Crear el Caso de Uso (el cerebro de la aplicación)
        LoginUseCase loginUseCase = new LoginUseCase(authAdapter);

        // 4. Crear y mostrar la Vista (la interfaz gráfica)
        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView(loginUseCase);
            loginView.setVisible(true);
        });
    }
}
