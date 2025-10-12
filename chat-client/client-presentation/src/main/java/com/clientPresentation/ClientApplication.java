package com.clientPresentation;

import com.clientApplication.factories.CommandFactory;
import com.clientApplication.ports.ServerGatewayPort;
import com.clientInfrastructure.adapters.TcpGatewayAdapter;
import com.clientInfrastructure.network.TcpClient;
import com.chatCommon.protocol.ProtocolParser;
import com.clientPresentation.views.MainClientView;

import javax.swing.*;

public class ClientApplication {
    public static void main(String[] args) {
        ProtocolParser parser = new ProtocolParser('|', '\\');
        TcpClient tcpClient = new TcpClient("localhost", 12345);

        // --- Puerto y Adaptador ---
        ServerGatewayPort gateway = new TcpGatewayAdapter(tcpClient, parser);

        // --- Comandos de Aplicación --
        CommandFactory commandFactory = new CommandFactory(gateway);

        // --- Presentación ---
        SwingUtilities.invokeLater(() -> {
            // La vista principal solo necesita la fábrica para funcionar.
            MainClientView mainView = new MainClientView(commandFactory);
            mainView.setVisible(true);
        });
    }
}
