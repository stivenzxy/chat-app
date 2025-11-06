package com.serverInfrastructure.adapters.peer.managers;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverApplication.ports.PeerConnectionObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class PeerObserverNotifier {
    
    private static final Logger logger = LoggerFactory.getLogger(PeerObserverNotifier.class);
    
    private final List<PeerConnectionObserver> observers = new CopyOnWriteArrayList<>();

    public void addObserver(PeerConnectionObserver observer) {
        observers.add(observer);
        logger.debug("Observador P2P agregado: {}", observer.getClass().getSimpleName());
    }

    public void removeObserver(PeerConnectionObserver observer) {
        observers.remove(observer);
        logger.debug("Observador P2P removido: {}", observer.getClass().getSimpleName());
    }

    public void notifyPeerConnected(ConnectedPeerInfo peerInfo) {
        for (PeerConnectionObserver observer : observers) {
            try {
                observer.onPeerConnected(peerInfo);
            } catch (Exception e) {
                logger.error("Error notificando conexión de peer a observador: {}", e.getMessage());
            }
        }
    }

    public void notifyPeerDisconnected(ConnectedPeerInfo peerInfo) {
        for (PeerConnectionObserver observer : observers) {
            try {
                observer.onPeerDisconnected(peerInfo);
            } catch (Exception e) {
                logger.error("Error notificando desconexión de peer a observador: {}", e.getMessage());
            }
        }
    }

    public void notifyPeerConnectionError(String peerId, String errorMessage) {
        for (PeerConnectionObserver observer : observers) {
            try {
                observer.onPeerConnectionError(peerId, errorMessage);
            } catch (Exception e) {
                logger.error("Error notificando error de conexión de peer a observador: {}", e.getMessage());
            }
        }
    }
}
