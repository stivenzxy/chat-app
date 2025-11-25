package com.serverInfrastructure.factories;

import com.serverInfrastructure.adapters.peer.managers.PeerConnectionManager;
import com.serverInfrastructure.adapters.peer.managers.PeerMessageRoutingManager;
import com.serverInfrastructure.adapters.peer.managers.PeerObserverNotifier;
import com.serverInfrastructure.adapters.peer.managers.PeerUserSyncManager;
import com.serverInfrastructure.adapters.peer.PeerTcpServerAdapter;
import com.serverInfrastructure.adapters.peer.callback.PeerServerCallbackConfigurator;
import com.serverInfrastructure.adapters.peer.connection.IncomingConnectionHandler;
import com.serverInfrastructure.adapters.peer.connection.OutgoingConnectionHandler;
import com.serverInfrastructure.adapters.peer.connection.PeerConnectionValidator;
import com.serverInfrastructure.adapters.peer.discovery.PeerAutoReconnectService;
import com.serverInfrastructure.adapters.peer.discovery.PeerDiscoveryHandler;
import com.serverInfrastructure.adapters.peer.lifecycle.LocalServerIdentityProvider;
import com.serverInfrastructure.adapters.peer.lifecycle.ServerLifecycleManager;
import com.serverInfrastructure.adapters.peer.managers.PeerPeerReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerUserReplicationManager;
import com.serverInfrastructure.adapters.peer.managers.PeerEntityReplicationManager;

import java.util.function.Consumer;

public class PeerTcpServerAdapterFactory {

    public static PeerTcpServerAdapter create() {
        return create(null);
    }
    
    public static PeerTcpServerAdapter create(Consumer<Void> userReplicationCallback) {
        PeerConnectionValidator validator = new PeerConnectionValidator();
        LocalServerIdentityProvider identityProvider = new LocalServerIdentityProvider();
        ServerLifecycleManager lifecycleManager = new ServerLifecycleManager();
        
        PeerConnectionManager connectionManager = PeerManagerFactory.createConnectionManager();
        PeerUserSyncManager userSyncManager = PeerManagerFactory.createUserSyncManager();
        if (userReplicationCallback != null) {
            userSyncManager.setUiUpdateCallback(userReplicationCallback);
        }
        PeerMessageRoutingManager messageRoutingManager = PeerManagerFactory.createMessageRoutingManager();
        PeerObserverNotifier observerNotifier = PeerManagerFactory.createObserverNotifier();
        PeerUserReplicationManager userReplicationManager = PeerManagerFactory.createUserReplicationManager(userReplicationCallback);
        PeerPeerReplicationManager peerReplicationManager = PeerManagerFactory.createPeerReplicationManager();
        PeerEntityReplicationManager entityReplicationManager = PeerManagerFactory.createEntityReplicationManager();

        IncomingConnectionHandler incomingHandler = new IncomingConnectionHandler(
            validator, connectionManager, observerNotifier
        );
        
        PeerDiscoveryHandler discoveryHandler = new PeerDiscoveryHandler(validator);
        PeerAutoReconnectService autoReconnectService = new PeerAutoReconnectService();

        OutgoingConnectionHandler outgoingHandler = new OutgoingConnectionHandler(
            validator, connectionManager, userSyncManager, observerNotifier, 
            discoveryHandler, messageRoutingManager, userReplicationManager, peerReplicationManager, entityReplicationManager
        );
        
        PeerServerCallbackConfigurator callbackConfigurator = new PeerServerCallbackConfigurator(
            incomingHandler, userSyncManager, messageRoutingManager, userReplicationManager, peerReplicationManager, entityReplicationManager
        );
        
        discoveryHandler.setConnectionCallback(outgoingHandler::connectToPeer);
        autoReconnectService.setConnectionCallback(outgoingHandler::connectToPeer);

        connectionManager.setObserverNotifier(observerNotifier);
        connectionManager.setUserSyncManager(userSyncManager);

        return new PeerTcpServerAdapter(
            lifecycleManager, identityProvider, incomingHandler, outgoingHandler,
            discoveryHandler, autoReconnectService, callbackConfigurator,
            connectionManager, userSyncManager, messageRoutingManager, observerNotifier,
            userReplicationManager, peerReplicationManager, entityReplicationManager
        );
    }
}
