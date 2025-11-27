package com.clientApplication.commands;

import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.util.Base64;
import java.util.List;

public class SendChannelAudioClientCommand implements ClientCommand<SendChannelAudioClientCommand.Request, Boolean> {
    public static class Request {
        public final String channelId;
        public final byte[] audio;

        public Request(String channelId, byte[] audio) {
            this.channelId = channelId;
            this.audio = audio;
        }
    }

    private final ServerGatewayPort gateway;

    public SendChannelAudioClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public Boolean execute(Request request) {
        if (request.audio == null || request.audio.length == 0)
            return false;
        try {
            String b64 = Base64.getEncoder().encodeToString(request.audio);
            List<String> parts = gateway.sendAndReceive("SEND_CHANNEL_AUDIO", request.channelId, b64);
            return !parts.isEmpty() && "OK".equalsIgnoreCase(parts.getFirst());
        } catch (Exception e) {
            return false;
        }
    }
}
