package com.serverPresentation.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverApplication.factories.ServiceFactory;
import com.serverPresentation.http.controllers.LogsRestController;
import com.serverPresentation.http.controllers.UsersRestController;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP REST API server using Javalin.
 * Exposes endpoints for external consumption (e.g., Angular frontend via Traefik).
 */
public class HttpRestServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpRestServer.class);
    private static final int DEFAULT_PORT = 8080;
    
    private final Javalin app;
    private final int port;

    public HttpRestServer(ServiceFactory serviceFactory, int port) {
        this.port = port;
        
        // Configure Jackson for proper JSON serialization
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // Create Javalin app with Jackson configuration
        this.app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
            config.http.defaultContentType = "application/json";
        });
        
        configureRoutes(serviceFactory);
    }

    public HttpRestServer(ServiceFactory serviceFactory) {
        this(serviceFactory, DEFAULT_PORT);
    }

    private void configureRoutes(ServiceFactory serviceFactory) {
        // Create controllers
        LogsRestController logsController = new LogsRestController(
                serviceFactory.createGetServerLogsService()
        );
        
        UsersRestController usersController = new UsersRestController(
                serviceFactory.createGetUsersPresentationService()
        );

        // Configure CORS for Angular frontend
        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization");
        });

        // Handle OPTIONS preflight requests
        app.options("/*", ctx -> ctx.status(200));

        // API routes
        app.get("/api/logs", logsController::getLogs);
        app.get("/api/users", usersController::getUsers);
        
        // Health check endpoint
        app.get("/api/health", ctx -> ctx.json(new HealthResponse("ok")));
        
        logger.info("HTTP REST routes configured");
    }

    public void start() {
        app.start(port);
        logger.info("HTTP REST API server started on port {}", port);
    }

    public void stop() {
        app.stop();
        logger.info("HTTP REST API server stopped");
    }

    public int getPort() {
        return port;
    }

    private record HealthResponse(String status) {}
}
