package com.clientInfrastructure.adapters;

import com.chatCommon.dto.auth.LoginRequest;
import com.chatCommon.dto.auth.LoginResponse;
import com.clientApplication.ports.ServerGatewayPort;
import com.clientInfrastructure.network.TcpClient;
import com.chatCommon.protocol.ProtocolParser;

import java.util.List;
public class TcpGatewayAdapter implements ServerGatewayPort {

    private final TcpClient tcpClient;
    private final ProtocolParser parser;

    public TcpGatewayAdapter(TcpClient tcpClient, ProtocolParser parser) {
        this.tcpClient = tcpClient;
        this.parser = parser;
    }

    @Override
    public List<String> sendAndReceive(List<String> requestParts) {
        // 1. Codificar las partes en un string de protocolo
        String messageToSend = parser.encode(requestParts.toArray(new String[0]));

        // 2. Enviar y recibir la respuesta cruda
        String rawResponse = tcpClient.sendMessage(messageToSend);

        if (rawResponse == null || rawResponse.isEmpty()) {
            return List.of("ERROR", "No se recibió respuesta del servidor.");
        }

        // 3. Decodificar la respuesta cruda en partes
        return parser.decode(rawResponse);
    }
}