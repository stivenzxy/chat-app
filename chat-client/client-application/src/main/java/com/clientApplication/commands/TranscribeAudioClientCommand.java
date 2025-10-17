package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.Base64;
import java.util.List;

public class TranscribeAudioClientCommand implements ClientCommand<byte[], String> {

    private final ServerGatewayPort gateway;

    public TranscribeAudioClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public String execute(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            return null;
        }

        try {
            String audioBase64 = Base64.getEncoder().encodeToString(audioData);
            List<String> responseParts = gateway.sendAndReceive("TRANSCRIBE_AUDIO", audioBase64);
            
            if (responseParts.isEmpty()) {
                return null;
            }
            
            String status = responseParts.get(0);
            
            if ("TRANSCRIPTION_RESULT".equals(status) && responseParts.size() > 1) {
                return responseParts.get(1);
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
