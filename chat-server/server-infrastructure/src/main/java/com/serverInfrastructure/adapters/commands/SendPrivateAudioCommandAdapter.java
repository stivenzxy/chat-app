package com.serverInfrastructure.adapters.commands;

import com.chatCommon.protocol.ProtocolParser;
import com.serverInfrastructure.adapters.ProtocolCommandAdapter;
import com.serverApplication.ports.peer.PeerMessageRouter;
import com.serverInfrastructure.network.ClientConnection;
import com.serverInfrastructure.network.TcpServer;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.services.AudioTranscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Adaptador que maneja el comando SEND_PRIVATE_AUDIO de clientes.
 * 
 * Implementa patrón Local-First con fallback P2P:
 * 1. Intenta entrega local (usuario en este servidor)
 * 2. Si falla, usa PeerMessageRouter para enrutar a servidor remoto
 * 
 * Este patrón optimiza el caso común (90% local) y provee robustez
 * evitando condiciones de carrera.
 */
public class SendPrivateAudioCommandAdapter implements ProtocolCommandAdapter {
    private static final Logger logger = LoggerFactory.getLogger(SendPrivateAudioCommandAdapter.class);

    private final TcpServer server;
    private final MessageDAO messageDAO = new MessageDAO();
    private final AudioTranscriptionService audioTranscriptionService = new AudioTranscriptionService();
    
    /**
     * Router para enrutamiento P2P de audios privados.
     * Se inyecta desde InfrastructureFactory, desacoplando este comando
     * de la implementación concreta de P2P (Dependency Inversion Principle).
     */
    private PeerMessageRouter peerRouter;

    public SendPrivateAudioCommandAdapter(TcpServer server) {
        this.server = server;
    }

    @Override
    public String getCommandName() {
        return "SEND_PRIVATE_AUDIO";
    }

    @Override
    public String execute(List<String> parts, ProtocolParser parser, ClientConnection connectionContext) {
        if (parts.size() < 3) {
            return parser.encode("ERROR", "Argumentos insuficientes para enviar audio.");
        }

        // Formato: SEND_PRIVATE_AUDIO|destinatario_username|audio_en_base64
        String senderConnectionId = connectionContext.getId();
        
        // Obtener username del usuario que envía el audio
        com.serverInfrastructure.observers.ActiveUserManager aum = com.serverInfrastructure.observers.ActiveUserManager.getInstance();
        String senderUsername = aum.getAllUserSessions().entrySet().stream()
            .filter(entry -> entry.getValue().stream()
                .anyMatch(user -> user.getId().equals(senderConnectionId)))
            .map(java.util.Map.Entry::getKey)
            .findFirst()
            .orElse(null);
            
        if (senderUsername == null) {
            return parser.encode("ERROR", "Remitente no válido");
        }
        
        String recipientUsername = parts.get(1);
        String audioBase64 = parts.get(2);
        
        String senderUserId = aum.getUserIdFromConnection(senderConnectionId);
        
        String forwardMessage = parser.encode("RECEIVE_PRIVATE_AUDIO", senderUsername, audioBase64);

        String senderInfo = getSenderInfo(connectionContext) + " [AUDIO]";

        // PASO 1: Intentar entrega local primero (patrón Local-First)
        // Optimiza el caso común (90% usuarios locales) con una sola operación
        boolean delivered = server.sendMessageToUser(recipientUsername, forwardMessage, senderInfo);

        // PASO 2: Si no está local, usar fallback P2P
        // Garantiza entrega a usuarios en servidores remotos
        if (!delivered && peerRouter != null) {
            logger.info("Usuario {} no está local, intentando enrutamiento P2P de audio...", recipientUsername);
            
            // Extraer el username real si viene con prefijo "Servidor X - username"
            String actualUsername = recipientUsername;
            if (recipientUsername.contains(" - ")) {
                actualUsername = recipientUsername.substring(recipientUsername.lastIndexOf(" - ") + 3);
                logger.info("Username extraído del prefijo: {} -> {}", recipientUsername, actualUsername);
            }
            
            // Construir mensaje de enrutamiento P2P
            // Formato: P2P_ROUTE_PRIVATE_AUDIO|senderUsername|recipientUsername|audioBase64
            String routeMessage = "P2P_ROUTE_PRIVATE_AUDIO|" + senderUsername + "|" + actualUsername + "|" + audioBase64;
            
            // Usar el router P2P inyectado (DIP)
            delivered = peerRouter.routeToPeer(actualUsername, routeMessage);
            
            if (delivered) {
                logger.info("Audio enrutado exitosamente a usuario remoto {}", actualUsername);
            } else {
                logger.warn("No se pudo enrutar audio a usuario remoto {}", actualUsername);
            }
        }

        if (delivered) {
            String recipientConnectionId = aum.getUserSessions(recipientUsername).stream()
                .findFirst()
                .map(user -> user.getId())
                .orElse(null);
            
            String recipientUserId = null;
            if (recipientConnectionId != null) {
                recipientUserId = aum.getUserIdFromConnection(recipientConnectionId);
            }
            
            if (senderUserId != null && recipientUserId != null) {
                int messageId = -1;
                byte[] audioData = null;
                try {
                    audioData = Base64.getDecoder().decode(audioBase64);
                    messageId = messageDAO.savePrivateAudioMessage(senderUserId, recipientUserId, audioData);
                    
                    final int finalMessageId = messageId;
                    final byte[] finalAudioData = audioData;
                    if (finalMessageId > 0 && finalAudioData != null) {
                        new Thread(() -> {
                            try {
                                String transcribedText = audioTranscriptionService.transcribeAudio(finalAudioData);
                                
                                if (transcribedText != null && !transcribedText.trim().isEmpty()) {
                                    messageDAO.saveTranscription(finalMessageId, "WAV", transcribedText);
                                }
                            } catch (Exception e) {
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    logger.error("Error al guardar mensaje de audio: {}", e.getMessage());
                }
            }
            
            String echoAudio = parser.encode("ECHO_SENT_AUDIO", recipientUsername, audioBase64);
            server.sendMessageToUserExceptSession(senderUsername, echoAudio, senderConnectionId, null);
            
            return parser.encode("OK", "Audio enviado.");
        } else {
            return parser.encode("ERROR", "El usuario no está conectado o no existe.");
        }
    }
    
    private String getSenderInfo(ClientConnection connection) {
        try {
            String ip = connection.getSocket().getInetAddress().getHostAddress();
            return connection.getId() + " | IP: " + ip;
        } catch (Exception e) {
            return connection.getId();
        }
    }
    
    /**
     * Inyecta el router P2P para enrutamiento de audios privados.
     * 
     * Este método será llamado por InfrastructureFactory durante la configuración,
     * implementando Dependency Injection para desacoplar este comando de la
     * implementación concreta de P2P.
     * 
     * @param router Implementación de PeerMessageRouter que encapsula
     *               la lógica de enrutamiento P2P
     */
    public void setPeerMessageRouter(PeerMessageRouter router) {
        this.peerRouter = router;
        logger.info("PeerMessageRouter configurado en SendPrivateAudioCommandAdapter");
    }
}