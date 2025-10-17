package com.clientInfrastructure.network;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.function.Consumer;

public class TcpClient {
    private final String host;
    private final int port;
    private static final Logger logger = LoggerFactory.getLogger(TcpClient.class);

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect(Consumer<String> onMessageReceived) {
        logger.info("Conectando al servidor en {}:{}", host, port);

        try {
            socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            listenerThread = new Thread(() -> {
                try {
                    String serverResponse;
                    while (!Thread.currentThread().isInterrupted() && (serverResponse = in.readLine()) != null) {
                        onMessageReceived.accept(serverResponse);
                    }
                } catch (IOException e) {
                    if (!socket.isClosed()) {
                        logger.error("La conexión con el servidor se ha perdido.", e);
                    }
                } finally {
                    logger.info("El hilo lector ha terminado.");
                }
            });
            listenerThread.setName("ClientListenerThread");
            listenerThread.start();
            logger.info("Conectado y escuchando al servidor.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendRequest(String request) {
        if (out != null) {
            logger.info("Enviando -> {}", request);
            out.println(request);
        } else {
            logger.warn("No se puede enviar el mensaje, no hay conexión activa.");
        }
    }

    public void disconnect() {
        logger.info("Desconectando del servidor...");
        try {
            if (socket != null && !socket.isClosed() && out != null) {
                out.println("LOGOUT");
                out.flush();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            if (listenerThread != null) listenerThread.interrupt();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.error("Error al cerrar la conexión.", e);
        }
    }
}