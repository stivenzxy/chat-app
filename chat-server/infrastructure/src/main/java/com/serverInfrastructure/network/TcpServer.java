package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;

import com.protocol.ProtocolParser;
import com.serverApplication.dto.LoginUserRequest;
import com.serverApplication.useCases.LoginService;

public class TcpServer {
    private final int port;
    private final LoginService loginService;
    private final ProtocolParser protocolParser; // El parser ahora es un miembro de la clase

    public TcpServer(int port, LoginService loginService) {
        this.port = port;
        this.loginService = loginService;
        // Instanciamos el parser con los caracteres que queramos
        this.protocolParser = new ProtocolParser('|', '\\');
    }

    public void start() throws IOException {
        // 1. Crea el socket del servidor que escuchará en el puerto especificado.
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor TCP iniciado y escuchando en el puerto " + port);

        // 2. Bucle infinito para aceptar conexiones de clientes de forma continua.
        while (true) {
            // 3. La llamada a accept() es bloqueante: el código se detiene aquí
            //    hasta que un cliente se conecta.
            Socket socket = serverSocket.accept();
            System.out.println("Cliente conectado desde " + socket.getInetAddress());

            // 4. Por cada cliente, se crea y lanza un nuevo hilo para manejarlo.
            //    Esto permite que el bucle principal vuelva inmediatamente a esperar
            //    por el siguiente cliente, permitiendo múltiples conexiones simultáneas.
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String message = in.readLine();
            if (message == null) return; // El cliente se desconectó

            System.out.println("Mensaje recibido: " + message);

            // 1. Usamos el parser para decodificar
            List<String> parts = protocolParser.decode(message);
            String response = processCommand(parts);

            // 2. Enviamos la respuesta
            out.println(response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String processCommand(List<String> parts) {
        if (parts.isEmpty() || parts.get(0).isEmpty()) {
            // 3. Usamos el parser para codificar las respuestas
            return protocolParser.encode("ERROR", "Comando inválido");
        }

        String command = parts.get(0);

        if ("LOGIN".equalsIgnoreCase(command) && parts.size() == 3) {
            String username = parts.get(1);
            String password = parts.get(2);

            LoginUserRequest loginRequest = new LoginUserRequest(username, password);
            boolean success = loginService.login(loginRequest);

            return success
                    ? protocolParser.encode("OK", "Login exitoso")
                    : protocolParser.encode("ERROR", "Credenciales inválidas");
        }

        return protocolParser.encode("ERROR", "Comando desconocido");
    }
}
