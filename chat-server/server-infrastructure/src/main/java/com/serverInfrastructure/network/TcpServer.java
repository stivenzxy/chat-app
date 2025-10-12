package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.services.CommandHandler;

public class TcpServer {
    private final int port;
    private final CommandHandler commandHandler;
    private final ProtocolParser protocolParser;
    private volatile ServerSocket serverSocket; // Volatile para visibilidad entre hilos

    public TcpServer(int port, CommandHandler commandHandler) {
        this.port = port;
        this.commandHandler = commandHandler;
        this.protocolParser = new ProtocolParser('|', '\\');
    }

    public void start() throws IOException {
        // Asignamos el serverSocket a la variable de instancia
        this.serverSocket = new ServerSocket(port);
        System.out.println("Servidor TCP iniciado y escuchando en el puerto " + port);

        // El bucle ahora depende de si el socket está cerrado
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado desde " + socket.getInetAddress());
                new Thread(() -> handleClient(socket)).start();
            } catch (SocketException e) {
                // Esto ocurre cuando llamamos a stop(). Es una forma limpia de salir del bucle.
                System.out.println("ServerSocket cerrado, deteniendo el servidor.");
            }
        }
    }

    /**
     * Detiene el servidor de forma segura desde otro hilo.
     */
    public void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error al cerrar el ServerSocket.");
            e.printStackTrace();
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
