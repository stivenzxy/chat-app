package com.clientInfrastructure.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TcpClient {
    private final String host;
    private final int port;

    public TcpClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Envía un único mensaje al servidor y espera una única línea como respuesta.
     * Abre y cierra la conexión para cada mensaje.
     *
     * @param message El string del mensaje a enviar.
     * @return El string de la respuesta recibida del servidor.
     * @throws RuntimeException si ocurre un error de comunicación.
     */
    public String sendMessage(String message) {
        // Usamos try-with-resources para asegurar que el socket y los streams se cierren automáticamente
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 1. Enviar el mensaje al servidor
            System.out.println("Enviando -> " + message);
            out.println(message);

            // 2. Esperar y leer la respuesta del servidor
            String response = in.readLine();
            System.out.println("Recibido <- " + response);
            return response;

        } catch (IOException e) {
            // 3. Si algo falla (ej. el servidor no responde), se lanza una excepción
            //    que será capturada por el TcpAuthAdapter.
            System.err.println("Error de comunicación TCP: " + e.getMessage());
            throw new RuntimeException("No se pudo comunicar con el servidor.", e);
        }
    }
}