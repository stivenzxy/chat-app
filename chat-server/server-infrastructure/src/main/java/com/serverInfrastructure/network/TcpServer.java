package com.serverInfrastructure.network;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.services.CommandHandler;

public class TcpServer {
    private final int port;
    private final CommandHandler commandHandler;
    private final ProtocolParser protocolParser;
    private volatile ServerSocket serverSocket;
    private final List<ConnectionObserver> listeners = new CopyOnWriteArrayList<>();
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);


    public TcpServer(int port, CommandHandler commandHandler) {
        this.port = port;
        this.commandHandler = commandHandler;
        this.protocolParser = new ProtocolParser('|', '\\');
    }

    public void addConnectionObserver(ConnectionObserver listener) {
        this.listeners.add(listener);
    }

    public void start() throws IOException {
        this.serverSocket = new ServerSocket(port);
        System.out.println("Servidor TCP iniciado y escuchando en el puerto " + port);

        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                String clientId = "cliente-" + connectionCounter.incrementAndGet();
                ClientConnection connection = new ClientConnection(clientId, socket.getInetAddress().getHostAddress(), socket);

                fireClientConnected(connection);
                new Thread(() -> handleClient(socket)).start();
            } catch (SocketException e) {
                System.out.println("ServerSocket cerrado, deteniendo el servidor.");
            }
        }
    }

    private void fireClientConnected(ClientConnection connection) {
        for (ConnectionObserver listener : listeners) {
            listener.onClientConnected(connection);
        }
    }

    private void fireClientDisconnected(ClientConnection connection) {
        for (ConnectionObserver listener : listeners) {
            listener.onClientDisconnected(connection);
        }
    }

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
