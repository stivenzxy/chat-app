package com.clientInfrastructure.persistence.config;

import com.chatCommon.utils.AppProperties;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionManager {
    private static volatile ConnectionManager instance;
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private final String user, pass, driver;
    private String urlTemplate;
    private static String activeUserId = "TEMP";
    private volatile boolean isInitialized = false;

    private ConnectionManager() {
        AppProperties props = new AppProperties("client-configuration");
        this.urlTemplate = props.getProperty("URL_TEMPLATE");
        this.user = props.getProperty("USER");
        this.pass = props.getProperty("PASSWORD");
        this.driver = props.getProperty("DRIVER");

        try {
            Class.forName(driver);
            // initDatabase() no se llama aquí, se llama después de configurar el usuario.
        } catch (ClassNotFoundException e) {
            logger.error("Error al cargar el driver de H2: {}", e.getMessage());
        }
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionManager.class) {
                if (instance == null) {
                    instance = new ConnectionManager();
                }
            }
        }
        return instance;
    }

    public static void configureForUser(String userId) {
        ConnectionManager manager = getInstance();
        if (manager.isInitialized && manager.activeUserId.equals(userId)) return;

        manager.activeUserId = userId;
        manager.initDatabase();
        manager.isInitialized = true;
    }

    public Connection getConnection() throws SQLException {
        String finalUrl = urlTemplate.replace("{USER_ID}", activeUserId);
        return DriverManager.getConnection(finalUrl, user, pass);
    }

    private void initDatabase() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("h2-client-db-schema.sql")) {
            if (inputStream == null) {
                throw new RuntimeException("No se pudo encontrar el archivo h2-client-db-schema.sql");
            }
            String scriptSql = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(scriptSql);
                logger.info("Base de datos H2 del cliente inicializada correctamente para usuario: {}.", activeUserId);
            }
        } catch (Exception e) {
            logger.error("Error al inicializar la base de datos H2 del cliente para {}", activeUserId, e);
            throw new RuntimeException(e);
        }
    }
}