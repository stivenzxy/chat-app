package com.serverPresentation.http.metrics;

import com.serverInfrastructure.adapters.TcpServerAdapter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmInfoMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gestiona las métricas de Prometheus para el servidor.
 * Responsable de configurar y registrar todas las métricas JVM, sistema y aplicación.
 */
public class MetricsManager {
    private static final Logger logger = LoggerFactory.getLogger(MetricsManager.class);
    
    private final PrometheusMeterRegistry prometheusRegistry;
    
    public MetricsManager() {
        this.prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registerJvmMetrics();
        registerSystemMetrics();
        logger.info("Prometheus metrics registry initialized");
    }
    
    /**
     * Registra métricas estándar de JVM
     */
    private void registerJvmMetrics() {
        new ClassLoaderMetrics().bindTo(prometheusRegistry);
        new JvmMemoryMetrics().bindTo(prometheusRegistry);
        new JvmGcMetrics().bindTo(prometheusRegistry);
        new JvmThreadMetrics().bindTo(prometheusRegistry);
        new JvmInfoMetrics().bindTo(prometheusRegistry);
    }
    
    /**
     * Registra métricas de sistema
     */
    private void registerSystemMetrics() {
        new ProcessorMetrics().bindTo(prometheusRegistry);
        new UptimeMetrics().bindTo(prometheusRegistry);
        new FileDescriptorMetrics().bindTo(prometheusRegistry);
    }
    
    /**
     * Registra métricas personalizadas de conexiones TCP
     * @param tcpServerAdapter Adaptador TCP del servidor
     */
    public void registerTcpMetrics(TcpServerAdapter tcpServerAdapter) {
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
    
    /**
     * Obtiene el registro de Prometheus
     * @return Registro de métricas de Prometheus
     */
    public PrometheusMeterRegistry getRegistry() {
        return prometheusRegistry;
    }
}
