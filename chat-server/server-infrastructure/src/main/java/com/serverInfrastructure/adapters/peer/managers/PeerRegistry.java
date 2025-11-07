package com.serverInfrastructure.adapters.peer.managers;

import com.serverApplication.dto.ConnectedPeerInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.chatCommon.utils.AppProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class PeerRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PeerRegistry.class);
    private static final String DEFAULT_FILE = "known_peers.txt";

    private final Path storagePath;
    private final Set<String> knownPeers = Collections.synchronizedSet(new HashSet<>());

    private static PeerRegistry instance;

    private PeerRegistry(Path path) {
        this.storagePath = path;
        loadFromDisk();
    }

    public static synchronized PeerRegistry getInstance() {
        if (instance == null) {
            Path p = Path.of(System.getProperty("user.dir"), DEFAULT_FILE);
            logger.debug("Inicializando PeerRegistry con path de almacenamiento: {}", p);
            instance = new PeerRegistry(p);
        }
        return instance;
    }

    private void loadFromDisk() {
        try {
            if (!Files.exists(storagePath)) {
                logger.info("No existe archivo de peers conocidos, se creará al guardar: {}", storagePath);
                return;
            }

            List<String> lines = Files.readAllLines(storagePath);
            for (String line : lines) {
                String t = line.trim();
                if (!t.isEmpty()) knownPeers.add(t);
            }
            logger.info("Cargados {} peers conocidos desde {}", knownPeers.size(), storagePath);
        } catch (Exception e) {
            logger.error("Error cargando peers conocidos: {}", e.getMessage());
        }
    }

    private int getConfiguredPeerPort() {
        try {
            AppProperties props = new AppProperties("server-configuration");
            return props.getInt("PEER_SERVER_PORT");
        } catch (Exception e) {
            logger.warn("No se pudo leer PEER_SERVER_PORT desde configuración: {}", e.getMessage());
            return -1;
        }
    }

    private boolean isPeerPortValid(String peerId) {
        try {
            if (peerId == null || peerId.isBlank()) return false;
            String[] parts = peerId.split(":");
            if (parts.length < 2) return false;
            int port = Integer.parseInt(parts[1]);
            // Reject typical ephemeral ports (49152-65535) which are assigned by OS for client sockets
            int EPHEMERAL_LOWER = 49152;
            int EPHEMERAL_UPPER = 65535;
            if (port >= EPHEMERAL_LOWER && port <= EPHEMERAL_UPPER) {
                return false;
            }
            // Accept any non-ephemeral port as valid peer P2P port. This allows peers to use different configured ports (e.g., 9095, 9096)
            return true;
        } catch (Exception e) {
            logger.debug("Error validando puerto de peer {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    private void persistToDisk() {
        try {
            Files.write(storagePath, knownPeers.stream().sorted().collect(Collectors.toList()),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Persistidos {} peers en {}", knownPeers.size(), storagePath);
        } catch (Exception e) {
            logger.error("Error persistiendo peers conocidos: {}", e.getMessage());
        }
    }

    public synchronized void addKnownPeer(ConnectedPeerInfo peerInfo) {
        if (peerInfo == null) return;
        String id = peerInfo.peerId();
        logger.debug("PeerRegistry.addKnownPeer called with ConnectedPeerInfo={}", id);
        // Only persist peers that match configured PEER_SERVER_PORT (avoid ephemeral client ports)
        if (!knownPeers.contains(id) && isPeerPortValid(id)) {
            knownPeers.add(id);
            logger.info("Nuevo peer conocido agregado: {}", id);
            persistToDisk();
        } else if (!isPeerPortValid(id)) {
            logger.debug("Ignorando registro de peer con puerto no válido: {}", id);
        }
    }

    public synchronized void addKnownPeer(String peerId) {
        if (peerId == null || peerId.isBlank()) return;
        logger.debug("PeerRegistry.addKnownPeer called with String={}", peerId);
        if (!knownPeers.contains(peerId) && isPeerPortValid(peerId)) {
            knownPeers.add(peerId);
            logger.info("Nuevo peer conocido agregado (string): {}", peerId);
            persistToDisk();
        } else if (!isPeerPortValid(peerId)) {
            logger.debug("Ignorando registro de peer con puerto no válido (string): {}", peerId);
        }
    }

    public synchronized Set<String> getKnownPeers() {
        return new HashSet<>(knownPeers);
    }

    /**
     * Returns only peers that are valid (match configured PEER_SERVER_PORT) or are already in registry.
     */
    public synchronized Set<String> getValidPeers() {
        return knownPeers.stream().filter(this::isPeerPortValid).collect(Collectors.toSet());
    }

    public synchronized void addAll(List<String> peers) {
        boolean changed = false;
        for (String p : peers) {
            if (p != null && !p.isBlank()) {
                if (knownPeers.add(p)) changed = true;
            }
        }
        logger.debug("PeerRegistry.addAll called, peers size={}, changed={}", peers == null ? 0 : peers.size(), changed);
        if (changed) persistToDisk();
    }

    public synchronized void clear() {
        knownPeers.clear();
        persistToDisk();
    }
}
