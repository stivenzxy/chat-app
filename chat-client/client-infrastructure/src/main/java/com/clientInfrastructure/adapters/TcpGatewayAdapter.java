package com.clientInfrastructure.adapters;

import com.clientApplication.ports.ServerGatewayPort;
import com.clientInfrastructure.network.TcpClient;
import com.chatCommon.protocol.ProtocolParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TcpGatewayAdapter implements ServerGatewayPort {
    private final TcpClient tcpClient;
    private final ProtocolParser parser;

    private static final Logger logger = LoggerFactory.getLogger(TcpGatewayAdapter.class);

    private final BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>(1);
    private Consumer<List<String>> asyncMessageListener;
    private Consumer<String> disconnectListener;

    public TcpGatewayAdapter(TcpClient tcpClient, ProtocolParser parser) {
        this.tcpClient = tcpClient;
        this.parser = parser;

        try {
            tcpClient.connect(message -> {
                List<String> parts = parser.decode(message);

                if (isDisconnectMessage(parts)) {
                    // Mensaje de desconexión del servidor
                    if (disconnectListener != null) {
                        String reason = parts.size() > 1 ? parts.get(1) : "Desconectado por el servidor";
                        disconnectListener.accept(reason);
                    }
                } else if (isResponse(parts)) {
                    try {
                        responseQueue.put(message);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.error("El hilo fue interrumpido al poner una respuesta en la cola.");
                    }
                } else {
                    if (asyncMessageListener != null) {
                        asyncMessageListener.accept(parts);
                    } else {
                        logger.warn("Mensaje asíncrono recibido pero no hay listener registrado: {}", message);
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("No se pudo conectar al servidor al iniciar el gateway.", e);
        }
    }

    @Override
    public List<String> sendAndReceive(String command, String... args) {
        try {
            List<String> allParts = new ArrayList<>();
            allParts.add(command);
            allParts.addAll(Arrays.asList(args));

            String requestToSend = parser.encode(allParts.toArray(new String[0]));

            responseQueue.clear();
            tcpClient.sendRequest(requestToSend);

            String rawResponse = responseQueue.poll(5, TimeUnit.SECONDS);

            if (rawResponse == null) {
                return List.of("ERROR", "El servidor no respondió a tiempo.");
            }

            return parser.decode(rawResponse);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of("ERROR", "La espera de respuesta fue interrumpida.");
        }
    }

    public void setAsyncMessageListener(Consumer<List<String>> listener) {
        this.asyncMessageListener = listener;
    }
    
    public void setDisconnectListener(Consumer<String> listener) {
        this.disconnectListener = listener;
    }

    private boolean isResponse(List<String> parts) {
        if (parts.isEmpty()) return false;
        String first = parts.getFirst().toUpperCase();
        return first.equals("OK") || first.equals("ERROR") || first.equals("ACK");
    }
    
    private boolean isDisconnectMessage(List<String> parts) {
        if (parts.isEmpty()) return false;
        return parts.getFirst().equalsIgnoreCase("DISCONNECT");
    }

    @Override
    public void disconnect() {
        if (tcpClient != null) {
            tcpClient.disconnect();
        }
    }
}