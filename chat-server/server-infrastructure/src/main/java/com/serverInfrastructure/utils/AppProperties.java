package com.serverInfrastructure.utils;

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
            tempRb = ResourceBundle.getBundle("server-configuration");
            logger.info("Archivo de configuración 'server-configuration.properties' cargado.");
        } catch (MissingResourceException exception) {
            logger.error("FATAL: No se pudo encontrar el archivo 'server-configuration.properties' en src/main/resources.", exception);
            throw new RuntimeException("No se pudo iniciar la aplicación por falta de configuración.", exception);
        }
        resourceBundle = tempRb;
    }

    public static String getProperty(String key) {
        return resourceBundle.getString(key);
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(resourceBundle.getString(key));
        } catch (Exception exception) {
            logger.warn("Clave {} no encontrada o inválida. Usando valor por defecto {}", key, defaultValue);
            return defaultValue;
        }
    }
}