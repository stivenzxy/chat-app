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

    // 1. Guardamos el estado de la conexión en campos de la clase
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listenerThread;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * 2. Establece la conexión e inicia un hilo para escuchar mensajes del servidor.
     *
     * @param onMessageReceived Un "notificador" que se ejecuta cada vez que llega un mensaje.
     */
    public void connect(Consumer<String> onMessageReceived) {
        logger.info("Conectando al servidor en {}:{}", host, port);

        try {
            socket = new Socket(host, port);

            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));


            // 3. Hilo Lector: su única tarea es escuchar al servidor
            listenerThread = new Thread(() -> {
                try {
                    String serverResponse;
                    while (!Thread.currentThread().isInterrupted() && (serverResponse = in.readLine()) != null) {
                        logger.info("Recibido <- {}", serverResponse);
                        // Cuando llega un mensaje, se lo pasamos al notificador
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

    /**
     * 5. Cierra todos los recursos y detiene el hilo lector.
     */
    public void disconnect() {
        logger.info("Desconectando del servidor...");
        try {
            if (listenerThread != null) listenerThread.interrupt();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            logger.error("Error al cerrar la conexión.", e);
        }
    }
}