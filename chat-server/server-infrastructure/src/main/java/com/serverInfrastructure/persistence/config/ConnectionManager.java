package com.serverInfrastructure.persistence.config;

import com.serverInfrastructure.persistence.exception.DatabaseException;
import com.chatCommon.utils.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class ConnectionManager {
    private static volatile ConnectionManager instance = null;
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    // --- CAMBIAR ESTOS CAMPOS DE STATIC A INSTANCE ---
    private final String DRIVER;
    private final String URL;
    private final String USER;
    private final String PASS;

    private ConnectionManager() {
        // --- INICIALIZAR AppProperties AQUÍ ---
        AppProperties props = new AppProperties("server-configuration");
        DRIVER = props.getProperty("DRIVER");
        URL = props.getProperty("URL");
        USER = props.getProperty("USER");
        PASS = props.getProperty("PASSWORD");

        try {
            Class.forName(DRIVER);
            initDatabase();
        } catch (ClassNotFoundException e) {
            logger.error("Error al cargar el driver de la Base de datos: {}", e.getMessage());
        }
    }

    /* synchronized blocks instance creation for only Thread at a time  */
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

    public Connection getConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException exception) {
            logger.error("Error al conectar con la Base de datos: {}", exception.getMessage());
            throw new DatabaseException("Error al obtener la conexión con la base de datos", 500);
        }
    }

    private void initDatabase() {
        try (InputStream inputStream = ConnectionManager.class.getClassLoader().getResourceAsStream("mysql-server-db-schema.sql")) {

            if (inputStream == null) {
                throw new DatabaseException("No se pudo encontrar el archivo database-schema.sql en los recursos.", 404);
            }

            String scriptSql = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));

            try (Connection conn = this.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(scriptSql);
                logger.info("Base de datos inicializada correctamente");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}