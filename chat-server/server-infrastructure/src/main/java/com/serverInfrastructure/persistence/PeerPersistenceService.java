package com.serverInfrastructure.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PeerPersistenceService {
    private static final Logger logger = LoggerFactory.getLogger(PeerPersistenceService.class);
    private static final String PEERS_FILE = "known_peers.txt";

    public Set<String> loadPeers() {
        File file = new File(PEERS_FILE);
        if (!file.exists()) {
            logger.info("No se encontró el archivo de peers conocidos '{}'. Se iniciará sin reconexiones.", PEERS_FILE);
            return Collections.emptySet();
        }

        // Usamos un Set concurrente para seguridad en hilos
        Set<String> peers = ConcurrentHashMap.newKeySet();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    peers.add(line.trim());
                }
            }
            logger.info("Cargados {} peers conocidos de {}", peers.size(), PEERS_FILE);
        } catch (IOException e) {
            logger.error("Error al cargar el archivo de peers conocidos: {}", e.getMessage());
        }
        return peers;
    }

    public void savePeers(Set<String> peers) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(PEERS_FILE))) {
            for (String peer : peers) {
                writer.write(peer);
                writer.newLine();
            }
            logger.info("Guardados {} peers conocidos en {}", peers.size(), PEERS_FILE);
        } catch (IOException e) {
            logger.error("Error al guardar el archivo de peers conocidos: {}", e.getMessage());
        }
    }
}