package com.serverPresentation.facades;

import com.chatCommon.utils.AppProperties;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.adapters.ServerNetworkAdapter;
import com.serverInfrastructure.adapters.TcpServerAdapter;
import com.serverInfrastructure.factories.DefaultServiceFactory;
import com.serverInfrastructure.factories.InfrastructureFactory;
import com.serverPresentation.controllers.UserController;
import com.serverPresentation.factories.PresentationFactory;
import com.serverPresentation.http.HttpRestServer;
import com.serverPresentation.http.clients.eureka.EurekaAutoDiscoveryClient;
import com.serverPresentation.services.EurekaService;
import com.serverPresentation.views.MainServerView;
import com.serverPresentation.views.dialogs.InstanceNameDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;

import static java.net.InetAddress.getLocalHost;

public class ServerStartupFacade {
    private static final Logger logger = LoggerFactory.getLogger(ServerStartupFacade.class);
    String eurekaUrl = EurekaAutoDiscoveryClient.discoverEureka();

    private final EurekaService eurekaService;

    public ServerStartupFacade() {
        this.eurekaService = new EurekaService();
    }
    
    public void start() {
        String instanceName = requestInstanceName();
        if (instanceName == null) {
            logger.info("Inicio cancelado por el usuario");
            System.exit(0);
            return;
        }
        
        DefaultServiceFactory serviceFactory = new DefaultServiceFactory();
        InfrastructureFactory infraFactory = new InfrastructureFactory(serviceFactory);
        TcpServerAdapter serverControl = new TcpServerAdapter(infraFactory);
        
        PresentationFactory presentationFactory = setupPresentationLayer(
            serviceFactory, infraFactory, serverControl
        );
        
        setupPeerNetwork(serviceFactory, infraFactory, serverControl, presentationFactory);
        
        AppProperties props = new AppProperties("server-configuration");
        int httpPort = props.getInt("HTTP_PORT");

        String serverIp = detectServerIp();
        registerInEureka(instanceName, serverIp, httpPort, eurekaUrl);

        startHttpServer(presentationFactory, serverControl, httpPort);

        showMainView(presentationFactory);
    }

    private String requestInstanceName() {
        try {
            final String[] result = new String[1];
            SwingUtilities.invokeAndWait(() -> {
                InstanceNameDialog dialog = new InstanceNameDialog(null);
                result[0] = dialog.showDialog();
            });
            return result[0];
        } catch (Exception e) {
            logger.error("Error mostrando diálogo de nombre de instancia", e);
            return null;
        }
    }

    private PresentationFactory setupPresentationLayer(
        DefaultServiceFactory serviceFactory,
        InfrastructureFactory infraFactory,
        TcpServerAdapter serverControl
    ) {
        PresentationFactory presentationFactory = new PresentationFactory(
            serviceFactory, infraFactory, serverControl, null
        );
        
        UserController userController = presentationFactory.createUserController();
        infraFactory.setUserReplicationCallback(v -> userController.getUserListUpdateCallback().run());
        
        return presentationFactory;
    }

    private void setupPeerNetwork(
        DefaultServiceFactory serviceFactory,
        InfrastructureFactory infraFactory,
        TcpServerAdapter serverControl,
        PresentationFactory presentationFactory
    ) {
        ServerNetworkAdapter singlePeerNetworkControl = infraFactory.createServerNetworkAdapter();
        
        serviceFactory.getReplicationNotifierProxy().setDelegate(singlePeerNetworkControl);
        serverControl.setPeerNetworkControl(singlePeerNetworkControl);
        presentationFactory.setServerNetworkControl(singlePeerNetworkControl);
    }
    

    private String detectServerIp() {
        return getLocalIpAddress();
    }
    

    private String getLocalIpAddress() {
        try {
            return getLocalHost().getHostAddress();
        } catch (Exception e) {
            logger.warn("No se pudo detectar IP local, usando localhost", e);
            return "127.0.0.1";
        }
    }

    private void registerInEureka(String instanceName, String serverIp, int port, String eurekaUrl) {
        try {
            eurekaService.register(instanceName, serverIp, port, eurekaUrl);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Ejecutando shutdown hook...");
                eurekaService.shutdown();
            }));
        } catch (Exception e) {
            logger.error("Error crítico al registrar en Eureka", e);
        }
    }

    private void startHttpServer(
        PresentationFactory presentationFactory,
        TcpServerAdapter serverControl,
        int httpPort
    ) {
        HttpRestServer httpServer = presentationFactory.createHttpRestServer(httpPort);
        httpServer.setTcpServerAdapter(serverControl);
        httpServer.start();
    }

    private void showMainView(PresentationFactory presentationFactory) {
        SwingUtilities.invokeLater(() -> {
            MainServerView view = presentationFactory.createMainServerView();
            view.setVisible(true);
        });
    }
}
