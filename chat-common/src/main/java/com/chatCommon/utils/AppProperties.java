// Ubicación: chat-common/src/main/java/com/chatCommon/utils/AppProperties.java
package com.chatCommon.utils;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppProperties {
    private static final Logger logger = LoggerFactory.getLogger(AppProperties.class);
    private final ResourceBundle resourceBundle; // Debe ser 'final' y no estático

    public AppProperties(String bundleName) {
        ResourceBundle tempRb;
        try {
            tempRb = ResourceBundle.getBundle(bundleName);
            logger.info("Archivo de configuración '{}' cargado.", bundleName);
        } catch (MissingResourceException exception) {
            logger.error("FATAL: No se pudo encontrar el archivo '{}' en los recursos.", bundleName, exception);
            throw new RuntimeException("No se pudo iniciar la aplicación por falta de configuración.", exception);
        }
        this.resourceBundle = tempRb;
    }

    public String getProperty(String key) {
        return resourceBundle.getString(key);
    }

    // Este método NO debe ser estático
    public int getInt(String key) {
        try {
            return Integer.parseInt(resourceBundle.getString(key));
        } catch (Exception exception) {
            logger.error("FATAL: Clave {} no encontrada o inválida en la configuración.", key);
            throw new RuntimeException("Configuración inválida: falta la clave " + key, exception);
        }
    }
}