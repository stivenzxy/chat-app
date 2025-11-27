package com.clientApplication.commands;

import com.chatCommon.dto.MessageDTO;
import com.chatCommon.dto.MessageType;
import com.clientApplication.commands.contract.ClientCommand;
import com.clientApplication.ports.ServerGatewayPort;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class GetChannelHistoryClientCommand implements ClientCommand<String, List<MessageDTO>> {

    private final ServerGatewayPort gateway;

    public GetChannelHistoryClientCommand(ServerGatewayPort gateway) {
        this.gateway = gateway;
    }

    @Override
    public List<MessageDTO> execute(String channelId) {
        try {
            List<String> responseParts = gateway.sendAndReceive(
                    "GET_CHANNEL_HISTORY",
                    channelId);

            if (!responseParts.isEmpty() && "OK".equals(responseParts.get(0))) {
                String messagesPayload = responseParts.size() > 1 ? responseParts.get(1) : "";
                if (messagesPayload.isEmpty()) {
                    return List.of();
                }
                return deserializeMessages(messagesPayload);
            } else {
                return List.of();
            }
        } catch (Exception e) {
            System.err.println("Error getting channel history: " + e.getMessage());
            return List.of();
        }
    }

    private List<MessageDTO> deserializeMessages(String messagesPayload) {
        List<MessageDTO> messages = new ArrayList<>();

        if (messagesPayload == null || messagesPayload.trim().isEmpty()) {
            return messages;
        }

        // Format:
        // authorId|messageType|content|audioContent(base64)|timestamp;;nextMessage...
        String[] messageParts = messagesPayload.split(";;");

        for (String messagePart : messageParts) {
            try {
                String[] fields = messagePart.split("\\|", -1);
                if (fields.length >= 5) {
                    String authorId = fields[0];
                    String messageType = fields[1];
                    String content = fields[2].replace("\\|", "|").replace("\\;", ";");
                    String audioBase64 = fields[3];
                    // timestamp in fields[4] - not used in MessageDTO constructor

                    if ("TEXT".equals(messageType)) {
                        messages.add(new MessageDTO(authorId, null, content));
                    } else if ("AUDIO".equals(messageType) && !audioBase64.isEmpty()) {
                        byte[] audioData = Base64.getDecoder().decode(audioBase64);
                        messages.add(new MessageDTO(authorId, null, audioData));
                    }
                }
            } catch (Exception e) {
                System.err.println("Error deserializing message: " + e.getMessage());
            }
        }

        return messages;
    }
}
