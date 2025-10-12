package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.useCases.LoginService;
import com.serverInfrastructure.services.CommandHandler;

public class TcpServer {
    private final int port;
    private final CommandHandler commandHandler;
    private final ProtocolParser protocolParser;

    public TcpServer(int port, CommandHandler commandHandler) {
        this.port = port;
        this.commandHandler = commandHandler;
        this.protocolParser = new ProtocolParser('|', '\\');
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Servidor TCP iniciado y escuchando en el puerto " + port);

        while (true) {
            Socket socket = serverSocket.accept();
            System.out.println("Cliente conectado desde " + socket.getInetAddress());
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            String message = in.readLine();
            if (message == null) return;

            System.out.println("Mensaje recibido: " + message);

            List<String> parts = protocolParser.decode(message);

            String response = commandHandler.process(parts);

            System.out.println("Enviando respuesta: " + response);
            out.println(response);
        } catch (IOException e) {
            System.err.println("Error de comunicación con el cliente: " + e.getMessage());
        } finally {
            // ... cerrar socket ...
        }
    }
}
