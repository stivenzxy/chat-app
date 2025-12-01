package com.serverPresentation.services;

import com.serverPresentation.http.clients.eureka.RegisterInEureka;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EurekaService {
    private static final Logger logger = LoggerFactory.getLogger(EurekaService.class);
    
    private RegisterInEureka eurekaRegistrar;

    public void register(String instanceName, String serverIp, int port, String eurekaUrl) {
        try {
            eurekaRegistrar = new RegisterInEureka(instanceName, serverIp, port, eurekaUrl);
            logger.info("Servidor registrado en Eureka como: {}", instanceName);
        } catch (Exception e) {
            logger.error("Error al registrar en Eureka: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register in Eureka", e);
        }
    }

    public void shutdown() {
        if (eurekaRegistrar != null) {
            try {
                eurekaRegistrar.shutdown();
                logger.info("Servidor des-registrado de Eureka");
            } catch (Exception e) {
                logger.error("Error al des-registrar de Eureka: {}", e.getMessage(), e);
            }
        }
    }
}
