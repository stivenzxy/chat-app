package com.serverPresentation.http.clients.eureka;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RegisterInEureka {

    private final String eurekaUrl;
    private final String appName;
    private final String instanceId;
    private final String serverIp;
    private final int port;
    private final HttpClient client;

    public RegisterInEureka(String appName, String serverIp, int port, String eurekaUrl) {
        this.eurekaUrl = eurekaUrl.endsWith("/") ? eurekaUrl : eurekaUrl + "/";
        this.appName = appName.toUpperCase();
        this.serverIp = serverIp;
        this.port = port;
        this.instanceId = appName + "-" + serverIp + ":" + port;
        this.client = HttpClient.newHttpClient();
        
        register(serverIp, port);
    }

    private void register(String host, int port) {
        try {
            String body = """
                    {
                      "instance": {
                        "hostName": "%s",
                        "app": "%s",
                        "vipAddress": "%s",
                        "ipAddr": "%s",
                        "status": "UP",
                        "port": { "$": %d, "@enabled": "true" },
                        "dataCenterInfo": {
                          "@class": "com.netflix.appinfo.InstanceInfo$DefaultDataCenterInfo",
                          "name": "MyOwn"
                        }
                      }
                    }
                    """.formatted(host, appName, appName, host, port);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(eurekaUrl + "apps/" + appName))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Instancia registrada en Eureka como: " + instanceId + " → HTTP " + response.statusCode());
        } catch (Exception e) {
            System.out.println("No se pudo registrar en Eureka (pero la app sigue funcionando): " + e.getMessage());
        }
    }

    public void shutdown() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .DELETE()
                .uri(URI.create(eurekaUrl + "apps/" + appName + "/" + instanceId))
                .build();

            client.send(request, HttpResponse.BodyHandlers.discarding());
            System.out.println("Instancia removida de Eureka");
        } catch (Exception e) {
            System.out.println("No se pudo eliminar la instancia en Eureka (apagando igualmente): " + e.getMessage());
        }
    }

    public String getInstances() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(eurekaUrl + "apps/" + appName))
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.body();
        } catch (Exception e) {
            return null;
        }
    }
}
