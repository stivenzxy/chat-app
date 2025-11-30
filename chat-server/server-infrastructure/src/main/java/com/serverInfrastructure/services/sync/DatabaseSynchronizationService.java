package com.serverInfrastructure.services.sync;

import com.serverApplication.dto.sync.*;
import com.serverDomain.entities.Channel;
import com.serverDomain.entities.ChannelInvite;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.ChannelInviteRepository;
import com.serverDomain.repositories.ChannelRepository;
import com.serverDomain.repositories.UserRepository;
import com.serverDomain.valueObjects.ChannelMember;
import com.serverDomain.valueObjects.Username;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DatabaseSynchronizationService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSynchronizationService.class);

    private final UserRepository userRepository;
    private final ChannelRepository channelRepository;
    private final ChannelInviteRepository channelInviteRepository;
    private final MessageDAO messageDAO;
    
    private java.util.function.Consumer<Void> onSyncCompleteCallback;

    public DatabaseSynchronizationService(UserRepository userRepository,
            ChannelRepository channelRepository,
            ChannelInviteRepository channelInviteRepository) {
        this.userRepository = userRepository;
        this.channelRepository = channelRepository;
        this.channelInviteRepository = channelInviteRepository;
        this.messageDAO = new MessageDAO();
    }
    
    /**
     * Sets callback to be invoked when database sync is complete.
     * Used to notify UI components to refresh their data.
     */
    public void setOnSyncCompleteCallback(java.util.function.Consumer<Void> callback) {
        this.onSyncCompleteCallback = callback;
    }

    public FullDatabaseSyncDTO exportFullDatabase() {
        logger.info("Iniciando exportación completa de base de datos...");

        List<UserSyncDTO> users = userRepository.findAll().stream()
                .map(u -> new UserSyncDTO(
                        u.getId(),
                        u.getUsername().value(),
                        u.getEmail().value(),
                        u.getPasswordHash(),
                        u.getPhotoData(),
                        u.getIpAddress(),
                        u.isReplicated(),
                        u.getOriginServerId(),
                        u.getLastSyncAt() != null ? u.getLastSyncAt().toLocalDateTime() : null,
                        u.getCreatedAt()))
                .collect(Collectors.toList());

        List<ChannelSyncDTO> channels = channelRepository.findAll().stream()
                .map(c -> new ChannelSyncDTO(
                        c.getId(),
                        c.getName(),
                        c.getOwnerId(),
                        c.getVisibility().name(),
                        c.getCreatedAt()))
                .collect(Collectors.toList());

        List<ChannelMemberSyncDTO> members = channelRepository.findAllMembers().stream()
                .map(m -> new ChannelMemberSyncDTO(m.channelId(), m.userId()))
                .collect(Collectors.toList());

        List<ChannelInviteSyncDTO> invites = channelInviteRepository.findAll().stream()
                .map(i -> new ChannelInviteSyncDTO(
                        i.getId(),
                        i.getChannelId(),
                        i.getInviterUserId(),
                        i.getInvitedUserId(),
                        i.getStatus().name(),
                        i.getCreatedAt()))
                .collect(Collectors.toList());

        List<MessageSyncDTO> messages = messageDAO.findAll();
        List<AudioTranscriptionSyncDTO> transcriptions = messageDAO.findAllTranscriptions();

        logger.info("Exportación completada: {} usuarios, {} canales, {} mensajes",
                users.size(), channels.size(), messages.size());

        return new FullDatabaseSyncDTO(users, channels, members, invites, messages, transcriptions);
    }

    public void importFullDatabase(FullDatabaseSyncDTO data) {
        logger.info("Iniciando importación completa de base de datos...");

        // 1. Usuarios
        int usersCount = 0;
        for (UserSyncDTO dto : data.users()) {
            try {
                if (userRepository.findByUsername(new Username(dto.username())).isEmpty()) {
                    User user = new User(dto.userId(), dto.username(), dto.email(), dto.passwordHash());
                    user.setPhotoData(dto.photoData());
                    user.setReplicated(true);
                    user.setOriginServerId(dto.originServerId());
                    userRepository.saveReplicatedUser(user);
                    usersCount++;
                }
            } catch (Exception e) {
                logger.error("Error importando usuario {}: {}", dto.username(), e.getMessage());
            }
        }

        // 2. Canales
        int channelsCount = 0;
        for (ChannelSyncDTO dto : data.channels()) {
            try {
                if (channelRepository.findById(dto.channelId()).isEmpty()) {
                    Channel channel = new Channel(
                            dto.channelId(),
                            dto.name(),
                            dto.ownerId(),
                            Channel.Visibility.valueOf(dto.visibility()),
                            dto.createdAt());
                    channelRepository.insertReplicated(channel);
                    channelsCount++;
                }
            } catch (Exception e) {
                logger.error("Error importando canal {}: {}", dto.channelId(), e.getMessage());
            }
        }

        // 3. Miembros de Canales
        for (ChannelMemberSyncDTO dto : data.channelMembers()) {
            try {
                if (!channelRepository.isMember(dto.channelId(), dto.userId())) {
                    channelRepository.addMember(dto.channelId(), dto.userId());
                }
            } catch (Exception e) {
                logger.error("Error importando miembro de canal: {}", e.getMessage());
            }
        }

        // 4. Invitaciones
        for (ChannelInviteSyncDTO dto : data.channelInvites()) {
            try {
                ChannelInvite invite = new ChannelInvite(
                        dto.inviteId(),
                        dto.channelId(),
                        dto.inviterUserId(),
                        dto.invitedUserId(),
                        ChannelInvite.Status.valueOf(dto.status()),
                        dto.createdAt());
                // Usamos insertReplicated que usa INSERT IGNORE o similar si el ID ya existe
                // O verificamos si existe, pero el repo no tiene findById para invites
                // facilmente expuesto
                // Asumimos que insertReplicated maneja duplicados (el DAO usa INSERT normal,
                // podría fallar si existe)
                // Deberíamos capturar excepción de clave duplicada
                channelInviteRepository.insertReplicated(invite);
            } catch (Exception e) {
                logger.debug("Error/duplicado importando invitación {}: {}", dto.inviteId(), e.getMessage());
            }
        }

        // 5. Mensajes
        int messagesCount = 0;
        for (MessageSyncDTO dto : data.messages()) {
            try {
                messageDAO.insertReplicated(dto);
                messagesCount++;
            } catch (Exception e) {
                logger.debug("Error/duplicado importando mensaje {}: {}", dto.messageId(), e.getMessage());
            }
        }

        // 6. Transcripciones
        for (AudioTranscriptionSyncDTO dto : data.transcriptions()) {
            try {
                messageDAO.insertTranscriptionReplicated(dto);
            } catch (Exception e) {
                logger.debug("Error/duplicado importando transcripción {}: {}", dto.messageId(), e.getMessage());
            }
        }

        logger.info("Importación completada. Nuevos: {} usuarios, {} canales, {} mensajes",
                usersCount, channelsCount, messagesCount);
        
        // Notificar a la UI que la sincronización está completa
        if (onSyncCompleteCallback != null && (usersCount > 0 || channelsCount > 0 || messagesCount > 0)) {
            try {
                onSyncCompleteCallback.accept(null);
                logger.info("UI notificada de sincronización completa de BD");
            } catch (Exception e) {
                logger.warn("Error notificando sincronización a UI: {}", e.getMessage());
            }
        }
    }
}
