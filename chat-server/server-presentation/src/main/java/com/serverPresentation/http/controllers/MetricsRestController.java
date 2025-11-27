package com.serverPresentation.http.controllers;

import io.javalin.http.Context;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsRestController {
    
    private final PrometheusMeterRegistry registry;
    
    public MetricsRestController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }
    
    /**
     * Endpoint para exponer métricas en formato Prometheus
     */
    public void getMetrics(Context ctx) {
        ctx.contentType("text/plain; version=0.0.4")
           .result(registry.scrape());
    }
}
