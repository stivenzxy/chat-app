package com.serverInfrastructure.adapters.peer.connection;

import com.serverInfrastructure.adapters.peer.utils.PeerIdParser;
import com.serverInfrastructure.utils.NetworkUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PeerConnectionValidator {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerConnectionValidator.class);
    
    private static final int EPHEMERAL_PORT_LOWER_BOUND = 49152;
    private static final int EPHEMERAL_PORT_UPPER_BOUND = 65535;
    

    public boolean isEphemeralPort(int port) {
        return port >= EPHEMERAL_PORT_LOWER_BOUND && port <= EPHEMERAL_PORT_UPPER_BOUND;
    }
    

    public boolean isValidPeerPort(String peerId) {
        try {
            if (peerId == null || peerId.isBlank()) {
                return false;
            }
            
            int port = PeerIdParser.extractPort(peerId);
            if (port == -1) {
                return false;
            }
            
            return !isEphemeralPort(port);
            
        } catch (Exception e) {
            logger.debug("Error validating peer port for {}: {}", peerId, e.getMessage());
            return false;
        }
    }

    public boolean isLocalAddress(String ip, int port, int localServerPort) {
        return NetworkUtils.isLocalAddress(ip, port, localServerPort);
    }
}