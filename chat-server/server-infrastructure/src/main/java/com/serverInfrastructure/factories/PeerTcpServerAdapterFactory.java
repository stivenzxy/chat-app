package com.serverInfrastructure.factories;

import com.serverInfrastructure.adapters.peer.Managers.PeerConnectionManager;
import com.serverInfrastructure.adapters.peer.Managers.PeerMessageRoutingManager;
import com.serverInfrastructure.adapters.peer.Managers.PeerObserverNotifier;
import com.serverInfrastructure.adapters.peer.Managers.PeerUserSyncManager;
import com.serverInfrastructure.adapters.peer.PeerTcpServerAdapter;
import com.serverInfrastructure.adapters.peer.callback.PeerServerCallbackConfigurator;
import com.serverInfrastructure.adapters.peer.connection.IncomingConnectionHandler;
import com.serverInfrastructure.adapters.peer.connection.OutgoingConnectionHandler;
import com.serverInfrastructure.adapters.peer.connection.PeerConnectionValidator;
import com.serverInfrastructure.adapters.peer.discovery.PeerAutoReconnectService;
import com.serverInfrastructure.adapters.peer.discovery.PeerDiscoveryHandler;
import com.serverInfrastructure.adapters.peer.lifecycle.LocalServerIdentityProvider;
import com.serverInfrastructure.adapters.peer.lifecycle.ServerLifecycleManager;

public class PeerTcpServerAdapterFactory {

    public static PeerTcpServerAdapter create() {
        PeerConnectionValidator validator = new PeerConnectionValidator();
        LocalServerIdentityProvider identityProvider = new LocalServerIdentityProvider();
        ServerLifecycleManager lifecycleManager = new ServerLifecycleManager();
        
        PeerConnectionManager connectionManager = PeerManagerFactory.createConnectionManager();
        PeerUserSyncManager userSyncManager = PeerManagerFactory.createUserSyncManager();
        PeerMessageRoutingManager messageRoutingManager = PeerManagerFactory.createMessageRoutingManager();
        PeerObserverNotifier observerNotifier = PeerManagerFactory.createObserverNotifier();

        IncomingConnectionHandler incomingHandler = new IncomingConnectionHandler(
            validator, connectionManager, observerNotifier
        );
        
        PeerDiscoveryHandler discoveryHandler = new PeerDiscoveryHandler(validator);
        PeerAutoReconnectService autoReconnectService = new PeerAutoReconnectService();

        OutgoingConnectionHandler outgoingHandler = new OutgoingConnectionHandler(
            validator, connectionManager, userSyncManager, observerNotifier, 
            discoveryHandler, messageRoutingManager
        );
        
        PeerServerCallbackConfigurator callbackConfigurator = new PeerServerCallbackConfigurator(
            incomingHandler, userSyncManager, messageRoutingManager
        );
        
        discoveryHandler.setConnectionCallback(outgoingHandler::connectToPeer);
        autoReconnectService.setConnectionCallback(outgoingHandler::connectToPeer);

        connectionManager.setObserverNotifier(observerNotifier);
        connectionManager.setUserSyncManager(userSyncManager);

        return new PeerTcpServerAdapter(
            lifecycleManager, identityProvider, incomingHandler, outgoingHandler,
            discoveryHandler, autoReconnectService, callbackConfigurator,
            connectionManager, userSyncManager, messageRoutingManager, observerNotifier
        );
    }
}
