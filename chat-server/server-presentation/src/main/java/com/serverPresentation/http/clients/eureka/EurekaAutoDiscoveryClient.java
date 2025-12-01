package com.serverPresentation.http.clients.eureka;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class EurekaAutoDiscoveryClient {
    public static String discoverEureka() {
        try (DatagramSocket socket = new DatagramSocket(8888, InetAddress.getByName("0.0.0.0"))) {
            socket.setSoTimeout(10000);

            byte[] buffer = new byte[256];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            System.out.println("[EurekaAutoDiscovery] Esperando broadcast del servidor Eureka...");

            socket.receive(packet);

            String msg = new String(packet.getData(), 0, packet.getLength());

            if (!msg.startsWith("EUREKA=")) {
                throw new RuntimeException("Broadcast inválido: " + msg);
            }

            String ipAndPort = msg.split("=")[1];
            String url = "http://" + ipAndPort + "/eureka/";

            System.out.println("[EurekaAutoDiscovery] Eureka encontrado en: " + url);
            return url;

        } catch (Exception e) {
            throw new RuntimeException("No se pudo descubrir el Eureka en la red LAN", e);
        }
    }
}
