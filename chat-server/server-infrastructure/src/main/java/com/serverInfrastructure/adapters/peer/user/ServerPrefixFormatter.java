package com.serverInfrastructure.adapters.peer.user;

import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;

public class ServerPrefixFormatter {
    
    private static final String SERVER_PREFIX_TEMPLATE = "Servidor %s - ";

    public static String createPrefix(String peerId) {
        String peerIp = PeerIdParser.extractIp(peerId);
        String identifier = (peerIp != null) ? peerIp : peerId;
        return String.format(SERVER_PREFIX_TEMPLATE, identifier);
    }
    
    public static String formatUsername(String peerId, String username) {
        return createPrefix(peerId) + username;
    }
}
