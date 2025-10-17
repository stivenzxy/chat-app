package com.serverPresentation.views.components;

import com.serverInfrastructure.services.ReportGenerationService;
import com.serverPresentation.factories.PresentationFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class ReportsPanel extends JPanel {

    private final PresentationFactory factory;
    private final ReportGenerationService reportService;

    public ReportsPanel(PresentationFactory factory) {
        this.factory = factory;
        this.reportService = new ReportGenerationService(); // Servicio para generar PDFs
        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Botones para cada informe
        JButton registeredUsersBtn = new JButton("Informe de Usuarios Registrados");
        JButton channelsBtn = new JButton("Informe de Canales y Miembros");
        JButton connectedUsersBtn = new JButton("Informe de Usuarios Conectados");
        JButton transcriptionsBtn = new JButton("Informe de Transcripciones de Audio");
        JButton logsBtn = new JButton("Informe de Logs del Servidor");

        // Estilo de botones
        styleButton(registeredUsersBtn);
        styleButton(channelsBtn);
        styleButton(connectedUsersBtn);
        styleButton(transcriptionsBtn);
        styleButton(logsBtn);

        // Añadir acciones a los botones
        registeredUsersBtn.addActionListener(e -> generateReport(reportService::generateRegisteredUsersReport, "registered_users_report.pdf"));
        channelsBtn.addActionListener(e -> generateReport(reportService::generateChannelsReport, "channels_report.pdf"));
        connectedUsersBtn.addActionListener(e -> generateReport(reportService::generateConnectedUsersReport, "connected_users_report.pdf"));
        transcriptionsBtn.addActionListener(e -> generateReport(reportService::generateTranscriptionsReport, "transcriptions_report.pdf"));
        logsBtn.addActionListener(e -> generateReport(reportService::generateLogsReport, "server_logs_report.pdf"));

        // Añadir botones al panel
        gbc.gridy = 0; add(registeredUsersBtn, gbc);
        gbc.gridy = 1; add(channelsBtn, gbc);
        gbc.gridy = 2; add(connectedUsersBtn, gbc);
        gbc.gridy = 3; add(transcriptionsBtn, gbc);
        gbc.gridy = 4; add(logsBtn, gbc);
    }

    private void styleButton(JButton button) {
        button.setFont(new Font("SansSerif", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(300, 40));
    }

    // Método genérico para generar y guardar el informe
    private void generateReport(ReportGenerationService.PdfReportGenerator generator, String defaultFileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar Informe PDF");
        fileChooser.setSelectedFile(new File(defaultFileName));

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            try {
                generator.generate(fileToSave.getAbsolutePath());
                JOptionPane.showMessageDialog(this, "Informe generado exitosamente en:\n" + fileToSave.getAbsolutePath(), "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error al generar el informe: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}