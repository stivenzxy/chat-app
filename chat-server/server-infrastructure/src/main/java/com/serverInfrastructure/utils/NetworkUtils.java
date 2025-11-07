package com.serverInfrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public final class NetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    private NetworkUtils() {}

    /**
     * Verifica de manera robusta si una combinación de host y puerto corresponde al servidor local.
     *
     * @param host El host a verificar (puede ser IP o nombre de host como 'localhost').
     * @param port El puerto a verificar.
     * @param localPeerPort El puerto en el que nuestro servidor P2P está escuchando.
     * @return true si la dirección corresponde al servidor local, false en caso contrario.
     */
    public static boolean isLocalAddress(String host, int port, int localPeerPort) {
        // Falla rápido: si el puerto no coincide, no puede ser nuestro servidor.
        if (port != localPeerPort) {
            return false;
        }

        try {
            // Resuelve el host proporcionado a una dirección IP.
            // Esto maneja casos como 'localhost' convirtiéndolo a '127.0.0.1'.
            InetAddress targetAddress = InetAddress.getByName(host);

            // Comprobación 1: ¿Es una dirección de loopback (localhost)?
            if (targetAddress.isLoopbackAddress()) {
                logger.debug("La dirección {} es una dirección de loopback local.", host);
                return true;
            }

            // Comprobación 2: Iterar sobre todas las interfaces de red de la máquina.
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress localAddress = inetAddresses.nextElement();
                    // Si la IP resuelta del host coincide con cualquiera de las IPs locales...
                    if (localAddress.equals(targetAddress)) {
                        logger.debug("La dirección {} coincide con la IP local {}", host, localAddress.getHostAddress());
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("No se pudo verificar la dirección del peer '{}': {}", host, e.getMessage());
            // En caso de duda, es mejor asumir que no es local para evitar perder una conexión válida.
            return false;
        }

        // Si ninguna comprobación tuvo éxito, no es una dirección local.
        return false;
    }
}