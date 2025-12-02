package com.serverInfrastructure.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public final class NetworkUtils {

    private static final Logger logger = LoggerFactory.getLogger(NetworkUtils.class);

    private NetworkUtils() {}

    public static boolean isLocalAddress(String host, int port, int localPeerPort) {
        if (port != localPeerPort) {
            return false;
        }

        try {
            InetAddress targetAddress = InetAddress.getByName(host);

            if (targetAddress.isLoopbackAddress()) {
                logger.debug("La dirección {} es una dirección de loopback local.", host);
                return true;
            }

            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress localAddress = inetAddresses.nextElement();
                    if (localAddress.equals(targetAddress)) {
                        logger.debug("La dirección {} coincide con la IP local {}", host, localAddress.getHostAddress());
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("No se pudo verificar la dirección del peer '{}': {}", host, e.getMessage());
            return false;
        }

        return false;
    }
}