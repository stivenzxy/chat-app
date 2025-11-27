package com.serverPresentation.http;

import com.chatCommon.utils.AppProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.serverApplication.factories.ServiceFactory;
import com.serverInfrastructure.adapters.TcpServerAdapter;
import com.serverPresentation.http.controllers.LogsRestController;
import com.serverPresentation.http.controllers.MetricsRestController;
import com.serverPresentation.http.controllers.UsersRestController;
import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class HttpRestServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpRestServer.class);
    private static final String SERVER_ID_FILE = "server-id.txt";
    
    // La URL del gateway se lee desde server-configuration.properties
    private final String gatewayUrl;
    
    private final Javalin app;
    private final int port;
    private String serverId;
    private final PrometheusMeterRegistry prometheusRegistry;
    private TcpServerAdapter tcpServerAdapter;

    public HttpRestServer(ServiceFactory serviceFactory, int port) {
        this.port = port;
        
        // Leer configuración del gateway desde server-configuration.properties
        AppProperties props = new AppProperties("server-configuration");
        this.gatewayUrl = props.getProperty("GATEWAY_URL");
        
        logger.info("Gateway URL configured: {}", gatewayUrl);
        
        // Configurar Prometheus Metrics
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        
        // Registrar métricas JVM estándar
        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);
        new ProcessorMetrics().bindTo(prometheusRegistry);
        new JvmInfoMetrics().bindTo(prometheusRegistry);  // jvm.info con version, vendor, runtime
        
        // Registrar métricas de sistema
        new UptimeMetrics().bindTo(prometheusRegistry);  // process.uptime, process.start.time
        new FileDescriptorMetrics().bindTo(prometheusRegistry);  // process.files.open, process.files.max
        
        logger.info("Prometheus metrics registry initialized");
        
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
        this(serviceFactory, 8080); // Puerto por defecto si no se especifica
    }
    
    /**
     * Configura el TcpServerAdapter para exponer métricas de conexiones TCP
     */
    public void setTcpServerAdapter(TcpServerAdapter tcpServerAdapter) {
        this.tcpServerAdapter = tcpServerAdapter;
        registerTcpMetrics();
    }
    
    /**
     * Registra métricas personalizadas de conexiones TCP en Prometheus
     */
    private void registerTcpMetrics() {
        if (tcpServerAdapter == null) {
            logger.warn("TcpServerAdapter no configurado, no se registrarán métricas de conexiones TCP");
            return;
        }
        
        // Métrica: Conexiones TCP actuales (clientes conectados)
        Gauge.builder("tcp_connections_active", tcpServerAdapter, TcpServerAdapter::getCurrentConnections)
            .description("Número de conexiones TCP activas (clientes conectados)")
            .tag("type", "client")
            .register(prometheusRegistry);
        
        // Métrica: Máximo de conexiones TCP permitidas
        Gauge.builder("tcp_connections_max", tcpServerAdapter, TcpServerAdapter::getMaxConnections)
            .description("Máximo de conexiones TCP permitidas")
            .tag("type", "client")
            .register(prometheusRegistry);
        
        // Métrica: Porcentaje de uso de conexiones
        Gauge.builder("tcp_connections_usage_ratio", tcpServerAdapter, adapter -> {
            int max = adapter.getMaxConnections();
            if (max == 0) return 0.0;
            return (double) adapter.getCurrentConnections() / max;
        })
            .description("Ratio de uso de conexiones TCP (0.0 - 1.0)")
            .tag("type", "client")
            .register(prometheusRegistry);
        
        logger.info("Métricas de conexiones TCP registradas en Prometheus");
    }

    private void configureRoutes(ServiceFactory serviceFactory) {
        LogsRestController logsController = new LogsRestController(
                serviceFactory.createGetServerLogsService()
        );
        
        UsersRestController usersController = new UsersRestController(
                serviceFactory.createGetUsersPresentationService()
        );
        
        MetricsRestController metricsController = new MetricsRestController(prometheusRegistry);

        app.before(ctx -> {
            ctx.header("Access-Control-Allow-Origin", "*");
            ctx.header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD, PATCH");
            ctx.header("Access-Control-Allow-Headers", "Content-Type, Authorization, Accept, Origin, X-Requested-With");
            ctx.header("Access-Control-Max-Age", "3600");
        });

        app.options("/*", ctx -> {
            ctx.status(204);
        });

        app.get("/api/logs", logsController::getLogs);
        app.get("/api/users", usersController::getUsers);
        app.get("/api/metrics", metricsController::getMetrics);
        
        app.get("/api/health", ctx -> ctx.json(new HealthResponse("ok")));
        
        // Endpoint para re-registro manual
        app.post("/api/register-in-gateway", ctx -> {
            try {
                registerInGateway();
                ctx.json(Map.of(
                    "status", "success",
                    "server_id", serverId != null ? serverId : "not_registered",
                    "message", "Successfully registered in gateway"
                ));
            } catch (Exception e) {
                logger.error("Failed to register in gateway", e);
                ctx.status(500).json(Map.of(
                    "status", "error",
                    "message", e.getMessage()
                ));
            }
        });
        
        logger.info("HTTP REST routes configured");
    }

    public void start() {
        app.start(port);
        logger.info("HTTP REST API server started on port {}", port);
        
        // Auto-registro en el gateway apenas inicia el servidor
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Esperar 2 segundos para que el servidor esté completamente listo
                registerInGateway();
            } catch (Exception e) {
                logger.error("Failed to auto-register in gateway on startup", e);
            }
        }).start();
    }

    public void stop() {
        app.stop();
        logger.info("HTTP REST API server stopped");
    }

    public int getPort() {
        return port;
    }
    
    /**
     * Detecta la IP local del servidor
     */
    private String getLocalIpAddress() {
        try {
            var interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                
                // Saltar interfaces inactivas, loopback, y virtuales (WSL, Docker, etc.)
                if (networkInterface.isLoopback() || !networkInterface.isUp() || networkInterface.isVirtual()) {
                    continue;
                }
                
                String name = networkInterface.getName().toLowerCase();
                // Saltar interfaces virtuales comunes: vEthernet, docker, vmware, vbox
                if (name.contains("vethernet") || name.contains("docker") || 
                    name.contains("vmware") || name.contains("vbox") || 
                    name.contains("virtual") || name.startsWith("veth")) {
                    continue;
                }
                
                var addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    
                    // Solo IPv4, no loopback, no link-local, y en rango privado (192.168.x.x, 10.x.x.x)
                    if (addr instanceof java.net.Inet4Address && 
                        !addr.isLoopbackAddress() && 
                        !addr.isLinkLocalAddress()) {
                        
                        String ip = addr.getHostAddress();
                        // Preferir IPs de redes privadas comunes
                        if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
                            logger.info("Detected local IP: {} (interface: {})", ip, networkInterface.getDisplayName());
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Could not detect real IP, using localhost", e);
        }
        
        return "127.0.0.1";
    }
    
    /**
     * Registra este servidor en el API Gateway
     */
    private void registerInGateway() throws IOException {
        // Leer la IP desde configuración
        AppProperties props = new AppProperties("server-configuration");
        String configuredIp = props.getProperty("GATEWAY_REGISTER_IP");
        
        String ip;
        if ("auto".equalsIgnoreCase(configuredIp)) {
            // Detectar automáticamente la IP local (para producción)
            ip = getLocalIpAddress();
        } else {
            // Usar la IP configurada (host.docker.internal para pruebas locales, o IP específica)
            ip = configuredIp;
        }
        
        logger.info("Registering server in gateway: {}:{}", ip, port);
        
        // Preparar datos de registro
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("ip", ip);
        requestData.put("port", port);
        
        ObjectMapper mapper = new ObjectMapper();
        String jsonData = mapper.writeValueAsString(requestData);
        
        // Enviar solicitud POST al gateway
        URL url = new URL(gatewayUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        
        try (var os = conn.getOutputStream()) {
            byte[] input = jsonData.getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        int responseCode = conn.getResponseCode();
        if (responseCode == 200 || responseCode == 201) {
            // Leer respuesta
            String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Map responseData = mapper.readValue(response, Map.class);
            
            serverId = (String) responseData.get("server_id");
            logger.info("Successfully registered as: {} ({})", serverId, responseData.get("status"));
            
            // Guardar server_id localmente para re-registro
            Files.writeString(Paths.get(SERVER_ID_FILE), serverId);
        } else {
            logger.error("Failed to register in gateway. Response code: {}", responseCode);
            throw new IOException("Gateway registration failed with code: " + responseCode);
        }
    }

    private record HealthResponse(String status) {}
}
