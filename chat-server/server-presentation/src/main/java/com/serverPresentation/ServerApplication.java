package com.serverPresentation;

import com.serverInfrastructure.network.TcpServer;
import com.serverPresentation.factories.ServerFactory;
import com.serverPresentation.views.UserManagementView;

import javax.swing.*;
import java.io.IOException;

public class ServerApplication {
    public static void main(String[] args) {
        // --- Configuración del Servidor ---
        final int SERVER_PORT = 12345; // Define el puerto aquí

        // --- Creación de Componentes ---
        ServerFactory factory = new ServerFactory();

        // --- Arranque del Servidor TCP en un Hilo Separado ---
        // Esto es CRÍTICO para que la interfaz gráfica no se congele.
        new Thread(() -> {
            System.out.println("Iniciando servidor TCP...");
            TcpServer tcpServer = factory.createTcpServer(SERVER_PORT);
            try {
                // El método start() es bloqueante, por eso debe estar en su propio hilo.
                tcpServer.start();
            } catch (IOException e) {
                System.err.println("No se pudo iniciar el servidor TCP en el puerto " + SERVER_PORT);
                e.printStackTrace();
                // Opcional: Mostrar un error en la GUI si falla el arranque
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "Error fatal: No se pudo iniciar el servidor de red.", "Error del Servidor", JOptionPane.ERROR_MESSAGE);
                    System.exit(1); // Salir si el servidor no puede arrancar
                });
            }
        }).start();

        // --- Arranque de la Interfaz Gráfica de Gestión ---
        // Esto se ejecuta en el hilo de Swing (Event Dispatch Thread).
        SwingUtilities.invokeLater(() -> {
            UserManagementView view = factory.createUserManagementView();
            view.setVisible(true);
        });
    }
}