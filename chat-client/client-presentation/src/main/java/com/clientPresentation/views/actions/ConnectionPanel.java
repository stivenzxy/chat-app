package com.clientPresentation.views.actions;

import com.chatCommon.protocol.ProtocolParser;
import com.chatCommon.viewResources.UiBuilder;
import com.clientApplication.factories.CommandFactory;
import com.clientApplication.ports.ServerGatewayPort;
import com.clientInfrastructure.adapters.TcpGatewayAdapter;
import com.clientInfrastructure.network.TcpClient;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.function.Consumer;

public class ConnectionPanel extends JPanel {

    private final Consumer<CommandFactory> onConnectionSuccess;
    private JTextField ipField;
    private JTextField portField;
    private JButton connectButton;

    public ConnectionPanel(Consumer<CommandFactory> onConnectionSuccess) {
        this.onConnectionSuccess = onConnectionSuccess;
        initComponents();
    }

    private void initComponents() {
        setBackground(new Color(249, 250, 251));
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(20, 20, 20, 20),
                BorderFactory.createLineBorder(new Color(210, 210, 210))
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        Border roundedBorder = UiBuilder.createRoundedBorder();

        ipField = new JTextField("localhost", 15);
        UiBuilder.styleField(ipField, roundedBorder);
        portField = new JTextField("12345", 15);
        UiBuilder.styleField(portField, roundedBorder);
        connectButton = new JButton("Conectar");
        UiBuilder.styleButton(connectButton, new Color(220, 38, 38));

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(UiBuilder.createLabelWithField("Dirección IP del Servidor", ipField), gbc);
        gbc.gridy++;
        add(UiBuilder.createLabelWithField("Puerto del Servidor", portField), gbc);
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        add(connectButton, gbc);

        connectButton.addActionListener(e -> onConnect());
    }

    private void onConnect() {
        String host = ipField.getText().trim();
        String portStr = portField.getText().trim();

        if (host.isEmpty() || portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "La IP y el Puerto no pueden estar vacíos.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);

            ProtocolParser parser = new ProtocolParser('|', '\\');
            TcpClient tcpClient = new TcpClient(host, port);
            ServerGatewayPort gateway = new TcpGatewayAdapter(tcpClient, parser);
            CommandFactory commandFactory = new CommandFactory(gateway);

            onConnectionSuccess.accept(commandFactory);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "El puerto debe ser un número válido.", "Error de Formato", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar al servidor.\n" + ex.getMessage(), "Error de Conexión", JOptionPane.ERROR_MESSAGE);
        }
    }
}