package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverApplication.dto.sync.MessageSyncDTO;
import com.serverDomain.repositories.ChannelRepository;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.persistence.dao.MessageDAO;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class GetChannelHistoryCommandAdapter implements ProtocolCommandAdapter {

    private final ChannelRepository channelRepository;
    private final MessageDAO messageDAO;

    public GetChannelHistoryCommandAdapter(ChannelRepository channelRepository) {
        this.channelRepository = channelRepository;
        this.messageDAO = new MessageDAO();
    }

    @Override
    public String getCommandName() {
        return "GET_CHANNEL_HISTORY";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        try {
            if (parts.size() < 2) {
                return parser.encode("ERROR", "Formato: GET_CHANNEL_HISTORY|channelId");
            }

            String channelId = parts.get(1);
            String requesterId = ActiveUserManager.getInstance().getUserIdFromConnection(connectionContext.getId());

            if (requesterId == null) {
                return parser.encode("ERROR", "Usuario no autenticado");
            }

            // Verify user is member of the channel
            boolean isMember = channelRepository.isMember(channelId, requesterId);

            if (!isMember) {
                return parser.encode("ERROR", "No eres miembro de este canal");
            }

            // Get messages from database
            List<MessageSyncDTO> messages = messageDAO.getChannelMessages(channelId);

            // Serialize messages
            String messagesPayload = serializeMessages(messages);
            return parser.encode("OK", messagesPayload);

        } catch (Exception e) {
            return parser.encode("ERROR", "Error obteniendo historial: " + e.getMessage());
        }
    }

    private String serializeMessages(List<MessageSyncDTO> messages) {
        // Format:
        // authorId|messageType|content|audioContent(base64)|timestamp;;nextMessage...
        return messages.stream()
                .map(msg -> {
                    String audioBase64 = msg.audioContent() != null
                            ? Base64.getEncoder().encodeToString(msg.audioContent())
                            : "";
                    String content = msg.content() != null ? msg.content() : "";

                    return String.join("|",
                            msg.authorId(),
                            msg.messageType(),
                            content.replace("|", "\\|").replace(";", "\\;"), // Escape delimiters
                            audioBase64,
                            msg.createdAt().toString());
                })
                .collect(Collectors.joining(";;"));
    }
}
