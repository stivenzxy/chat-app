package com.serverInfrastructure.adapters;

import com.serverApplication.dto.ConnectedPeerInfo;
import com.serverApplication.ports.PeerConnectionObserver;
import com.serverApplication.ports.peer.PeerNetworkControl;
import com.serverInfrastructure.adapters.peer.PeerTcpServerAdapter;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.observers.PeerUserSyncObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ServerNetworkAdapter implements PeerNetworkControl {
    private static final Logger logger = LoggerFactory.getLogger(ServerNetworkAdapter.class);
    private final PeerTcpServerAdapter peerAdapter;
    
    public ServerNetworkAdapter() {
        this.peerAdapter = PeerTcpServerAdapter.create();

        PeerUserSyncObserver syncObserver = new PeerUserSyncObserver(this.peerAdapter);
        ActiveUserManager.getInstance().addObserver(syncObserver);
        
        logger.info("ServerNetworkAdapter inicializado - Gestión P2P exclusiva con sincronización automática de usuarios");
    }

    @Override
    public boolean startPeerServer() {
        logger.info("Iniciando servidor P2P (puerto desde configuración)");
        return peerAdapter.startPeerServer();
    }
    
    @Override
    public void stopPeerServer() {
        logger.info("Deteniendo servidor P2P");
        peerAdapter.stopPeerServer();
    }
    
    @Override
    public boolean connectToPeer(String ip, int port) {
        logger.info("Conectando a peer {}:{}", ip, port);
        return peerAdapter.connectToPeer(ip, port);
    }
    
    @Override
    public boolean disconnectFromPeer(String peerId) {
        logger.info("Desconectando peer {}", peerId);
        return peerAdapter.disconnectFromPeer(peerId);
    }
    
    @Override
    public int disconnectFromPeers(List<String> peerIds) {
        logger.info("Desconectando multiples {} peers", peerIds.size());
        return peerAdapter.disconnectFromPeers(peerIds);
    }
    
    @Override
    public List<ConnectedPeerInfo> getConnectedPeers() {
        return peerAdapter.getConnectedPeers();
    }
    
    @Override
    public boolean isPeerServerRunning() {
        return peerAdapter.isPeerServerRunning();
    }
    
    @Override
    public int getPeerServerPort() {
        return peerAdapter.getPeerServerPort();
    }
    
    @Override
    public int getCurrentPeerConnections() {
        return peerAdapter.getCurrentPeerConnections();
    }
    
    @Override
    public boolean isConnectedToPeer(String peerId) {
        return peerAdapter.isConnectedToPeer(peerId);
    }
    
    @Override
    public boolean sendMessageToPeer(String peerId, String message) {
        return peerAdapter.sendMessageToPeer(peerId, message);
    }
    
    @Override
    public int broadcastToPeers(String message) {
        return peerAdapter.broadcastToPeers(message);
    }
    
    @Override
    public void addPeerConnectionObserver(PeerConnectionObserver observer) {
        peerAdapter.addPeerConnectionObserver(observer);
    }
    
    @Override
    public void removePeerConnectionObserver(PeerConnectionObserver observer) {
        peerAdapter.removePeerConnectionObserver(observer);
    }
    
    @Override
    public void notifyUserChangeToPeers(String username, String action) {
        peerAdapter.notifyUserChangeToPeers(username, action);
    }
    
    @Override
    public Map<String, List<String>> getAllUsersAcrossPeers() {
        return peerAdapter.getAllUsersAcrossPeers();
    }

    public String getRemoteUserPhoto(String username) {
        return peerAdapter.getRemoteUserPhoto(username);
    }

    public void registerClientBroadcast(Consumer<String> broadcastCallback) {
        peerAdapter.setClientBroadcastCallback(broadcastCallback);
        logger.info("Callback de broadcast registrado en ServerNetworkAdapter");
    }

    public boolean routePrivateMessageToPeer(String recipientUsername, String routeMessage) {
        logger.info("Enrutando el mensaje de {} utilizando el PeerAdapter", recipientUsername);
        return peerAdapter.routePrivateMessageToPeer(recipientUsername, routeMessage);
    }

    public boolean routePrivateAudioToPeer(String recipientUsername, String routeMessage) {
        logger.info("Enrutando audio para {} utilizando PeerAdapter", recipientUsername);
        return peerAdapter.routePrivateAudioToPeer(recipientUsername, routeMessage);
    }
}