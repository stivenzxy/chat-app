package com.clientInfrastructure.adapters;

import com.clientApplication.ports.ServerGatewayPort;
import com.clientInfrastructure.network.TcpClient;
import com.chatCommon.protocol.ProtocolParser;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TcpGatewayAdapter implements ServerGatewayPort {
    private final TcpClient tcpClient;
    private final ProtocolParser parser;

    // 1. "Buzón" para sincronizar la respuesta entre el hilo lector y el hilo principal.
    // Una capacidad de 1 es suficiente para una petición-respuesta a la vez.
    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>(1);

    public TcpGatewayAdapter(TcpClient tcpClient, ProtocolParser parser) {
        this.tcpClient = tcpClient;
        this.parser = parser;

        try {
            // Le decimos al cliente que ponga los mensajes recibidos en nuestra cola
            tcpClient.connect(message -> {
                try {
                    // Esta es la llamada que puede ser interrumpida
                    responseQueue.put(message);
                } catch (InterruptedException e) {
                    // Si el hilo es interrumpido, restauramos la bandera de interrupción
                    Thread.currentThread().interrupt();
                    System.err.println("El hilo de escucha fue interrumpido al intentar poner un mensaje en la cola.");
                }
            });
        } catch (Exception e) {
            // Si la conexión inicial falla, la aplicación no puede continuar.
            throw new RuntimeException("No se pudo conectar al servidor al iniciar el gateway.", e);
        }
    }

    @Override
    public List<String> sendAndReceive(List<String> requestParts) {
        try {
            String messageToSend = parser.encode(requestParts.toArray(new String[0]));

            // 3. Limpiamos la cola y enviamos el mensaje.
            responseQueue.clear();
            tcpClient.sendMessage(messageToSend);

            // 4. Esperamos la respuesta en la cola por un tiempo máximo (ej. 5 segundos).
            // poll() es mejor que take() porque evita que la app se congele indefinidamente.
            String rawResponse = responseQueue.poll(5, TimeUnit.SECONDS);

            if (rawResponse == null) {
                // Timeout: el servidor no respondió a tiempo.
                return List.of("ERROR", "El servidor no respondió a tiempo.");
            }

            // 5. Si llega una respuesta, la decodificamos y la devolvemos.
            return parser.decode(rawResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of("ERROR", "La espera de respuesta fue interrumpida.");
        }
    }
}