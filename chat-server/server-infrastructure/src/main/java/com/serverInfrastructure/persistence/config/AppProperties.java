package com.serverInfrastructure.persistence.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class AppProperties {
    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);
    private static final ResourceBundle resourceBundle;

    static {
        ResourceBundle tempRb = null;
        try {
            tempRb = ResourceBundle.getBundle("mysql-configuration");
            logger.info("Archivo de configuración 'mysql-configuration.properties' cargado.");
        } catch (MissingResourceException e) {
            logger.error("FATAL: No se pudo encontrar el archivo 'mysql-configuration.properties' en src/main/resources.", e);
            throw new RuntimeException("No se pudo iniciar la aplicación por falta de configuración.", e);
        }
        resourceBundle = tempRb;
    }

    public static String getProperty(String key) {
        return resourceBundle.getString(key);
    }
}