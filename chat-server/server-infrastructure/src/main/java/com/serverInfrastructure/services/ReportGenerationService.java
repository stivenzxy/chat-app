package com.serverInfrastructure.services;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue; // Corregido el import si había algún error
import com.serverApplication.dto.UserPresentationDTO;
import com.serverDomain.entities.Channel;
import com.serverDomain.entities.User;
import com.serverDomain.repositories.ChannelRepository;
import com.serverDomain.repositories.UserRepository;
import com.serverInfrastructure.observers.ActiveUserManager;
import com.serverInfrastructure.persistence.dao.MessageDAO;
import com.serverInfrastructure.persistence.repository.ChannelRepositoryImpl;
import com.serverInfrastructure.persistence.repository.UserManagementRepository;
import com.serverApplication.mappers.UserListMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ReportGenerationService {

    private final UserRepository userRepository = new UserManagementRepository();
    private final ChannelRepository channelRepository = new ChannelRepositoryImpl();

    @FunctionalInterface
    public interface PdfReportGenerator {
        void generate(String path) throws Exception;
    }

    // --- Informe 1: Usuarios Registrados ---
    public void generateRegisteredUsersReport(String path) throws Exception {
        List<User> users = userRepository.findAll();
        List<UserPresentationDTO> userDTOs = UserListMapper.toDTOList(users);

        try (PdfWriter writer = new PdfWriter(path);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            addTitle(document, "Informe de Usuarios Registrados");

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Username");
            table.addHeaderCell("Email");
            table.addHeaderCell("Fecha de Creación");

            for (UserPresentationDTO user : userDTOs) {
                table.addCell(user.getUsername());
                table.addCell(user.getEmail());
                table.addCell(formatDate(user.getCreatedAt()));
            }
            document.add(table);
        }
    }

    // --- Informe 2: Canales y Miembros ---
    public void generateChannelsReport(String path) throws Exception {
        List<Channel> channels = channelRepository.findAll();

        try (PdfWriter writer = new PdfWriter(path);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            addTitle(document, "Informe de Canales y Miembros");

            for (Channel channel : channels) {
                document.add(new Paragraph("Canal: " + channel.getName()).setBold().setMarginTop(15));

                List<String> members = channelRepository.findMemberUsernames(channel.getId());
                Table table = new Table(UnitValue.createPercentArray(new float[]{1}));
                table.setWidth(UnitValue.createPercentValue(50));
                table.addHeaderCell("Miembros Vinculados");

                if (members.isEmpty()) {
                    table.addCell("No hay miembros.");
                } else {
                    for (String member : members) {
                        table.addCell(member);
                    }
                }
                document.add(table);
            }
        }
    }

    // --- Informe 3: Usuarios Conectados ---
    public void generateConnectedUsersReport(String path) throws Exception {
        Map<String, User> activeUsers = ActiveUserManager.getInstance().getActiveUsers();

        try (PdfWriter writer = new PdfWriter(path);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            addTitle(document, "Informe de Usuarios Conectados (" + activeUsers.size() + ")");

            Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Username");
            table.addHeaderCell("IP Address");

            for (User user : activeUsers.values()) {
                table.addCell(user.getUsername().value());
                table.addCell(user.getIpAddress());
            }
            document.add(table);
        }
    }

    // --- Informe 4: Transcripciones de Audio ---
    public void generateTranscriptionsReport(String path) throws Exception {
        MessageDAO messageDAO = new MessageDAO();
        Map<String, String> transcriptions = messageDAO.getAllTranscriptions();

        try (PdfWriter writer = new PdfWriter(path);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            addTitle(document, "Informe de Transcripciones de Audio");

            Table table = new Table(UnitValue.createPercentArray(new float[]{1.5f, 1.5f, 3}));
            table.setWidth(UnitValue.createPercentValue(100));
            table.addHeaderCell("Autor y Fecha");
            table.addHeaderCell("Origen de Comunicación");
            table.addHeaderCell("Texto Transcrito");

            if (transcriptions.isEmpty()) {
                document.add(new Paragraph("No hay transcripciones guardadas en la base de datos."));
            } else {
                transcriptions.forEach((key, value) -> {
                    String comunicacion = "";
                    String autorYFecha = key;
                    
                    int comunicacionStart = key.lastIndexOf("[");
                    int comunicacionEnd = key.lastIndexOf("]");
                    if (comunicacionStart != -1 && comunicacionEnd != -1 && comunicacionEnd > comunicacionStart) {
                        comunicacion = key.substring(comunicacionStart + 1, comunicacionEnd);
                        autorYFecha = key.substring(0, comunicacionStart).trim();
                    }
                    
                    table.addCell(autorYFecha);
                    table.addCell(comunicacion);
                    table.addCell(value);
                });
                document.add(table);
            }
        }
    }

    // --- Informe 5: Logs del Servidor ---
    public void generateLogsReport(String path) throws Exception {
        String logFilePath = "server.log";

        try (PdfWriter writer = new PdfWriter(path);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            addTitle(document, "Informe de Logs del Servidor");

            try (BufferedReader reader = new BufferedReader(new FileReader(logFilePath))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    document.add(new Paragraph(line).setFontSize(8));
                }
            } catch (Exception e) {
                document.add(new Paragraph("No se pudo leer el archivo de logs. Asegúrate de que 'server.log' exista y logback esté configurado."));
            }
        }
    }


    // --- Métodos de ayuda ---
    private void addTitle(Document document, String titleText) {
        Paragraph title = new Paragraph(titleText)
                .setFontSize(18)
                .setBold()
                .setMarginBottom(10);
        document.add(title);
        document.add(new Paragraph("Generado el: " + formatDate(LocalDateTime.now())).setFontSize(10).setItalic());
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}