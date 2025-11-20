package com.serverInfrastructure.adapters.peer.user;

import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;

/**
 * Formats server prefixes for remote usernames.
 * Centralizes the logic for creating "Servidor X - username" format.
 */
public class ServerPrefixFormatter {
    
    private static final String SERVER_PREFIX_TEMPLATE = "Servidor %s - ";
    
    /**
     * Creates a server prefix from a peer ID.
     * Extracts IP from peerId and formats it as "Servidor {ip} - "
     */
    public static String createPrefix(String peerId) {
        String peerIp = PeerIdParser.extractIp(peerId);
        String identifier = (peerIp != null) ? peerIp : peerId;
        return String.format(SERVER_PREFIX_TEMPLATE, identifier);
    }
    
    /**
     * Formats a username with server prefix.
     */
    public static String formatUsername(String peerId, String username) {
        return createPrefix(peerId) + username;
    }
}
