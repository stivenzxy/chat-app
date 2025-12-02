package com.serverPresentation.http.controllers;

import io.javalin.http.Context;
import io.micrometer.prometheus.PrometheusMeterRegistry;

public class MetricsRestController {
    
    private final PrometheusMeterRegistry registry;
    
    public MetricsRestController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    public void getMetrics(Context ctx) {
        ctx.contentType("text/plain; version=0.0.4")
           .result(registry.scrape());
    }
}
