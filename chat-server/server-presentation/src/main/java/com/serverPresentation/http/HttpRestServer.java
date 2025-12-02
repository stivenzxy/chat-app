package com.serverPresentation.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.adapters.TcpServerAdapter;
import com.serverPresentation.http.controllers.LogsRestController;
import com.serverPresentation.http.controllers.MetricsRestController;
import com.serverPresentation.http.controllers.UsersRestController;
import com.serverPresentation.http.metrics.MetricsManager;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpRestServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpRestServer.class);
    
    private final Javalin app;
    private final int port;
    private final MetricsManager metricsManager;

    public HttpRestServer(ServiceFactory serviceFactory, int port) {
        this.port = port;
        this.metricsManager = new MetricsManager();
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        this.app = Javalin.create(config -> {
            config.jsonMapper(new JavalinJackson());
            config.http.defaultContentType = "application/json";
        });
        
        configureRoutes(serviceFactory);
    }

    public HttpRestServer(ServiceFactory serviceFactory) {
        this(serviceFactory, 8080);
    }
    
    public void setTcpServerAdapter(TcpServerAdapter tcpServerAdapter) {
        metricsManager.registerTcpMetrics(tcpServerAdapter);
    }

    private void configureRoutes(ServiceFactory serviceFactory) {
        LogsRestController logsController = new LogsRestController(
                serviceFactory.createGetServerLogsService()
        );
        
        UsersRestController usersController = new UsersRestController(
                serviceFactory.createGetUsersPresentationService()
        );
        
        MetricsRestController metricsController = new MetricsRestController(metricsManager.getRegistry());

        app.get("/api/logs", logsController::getLogs);
        app.get("/api/users", usersController::getUsers);
        app.get("/api/metrics", metricsController::getMetrics);
        app.get("/api/health", ctx -> ctx.json(new HealthResponse("Server is healthy")));
        
        logger.info("HTTP REST routes configured");
    }

    public void start() {
        app.start(port);
        logger.info("Servidor HTTP inicializado en el puerto: {}", port);
    }

    public void stop() {
        app.stop();
        logger.info("Servidor HTTP detenido");
    }

    public int getPort() {
        return port;
    }

    private record HealthResponse(String status) {}
}
