package com.serverInfrastructure.logging;

import com.serverApplication.dto.LogEntryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class LogFileReader {
    private static final Logger logger = LoggerFactory.getLogger(LogFileReader.class);
    
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}) \\[([^\\]]+)\\] (\\w+)\\s+([^ ]+) - (.+)$"
    );
    
    private static final DateTimeFormatter LOGBACK_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private final String logFilePath;

    public LogFileReader() {
        this("server.log");
    }
    
    public LogFileReader(String logFilePath) {
        this.logFilePath = logFilePath;
    }
    
    public List<LogEntryDTO> readAllLogs() {
        return readLogs(Integer.MAX_VALUE);
    }
    
    public List<LogEntryDTO> readRecentLogs(int maxEntries) {
        return readLogs(maxEntries);
    }
    
    public List<LogEntryDTO> readLogsByLevel(String level) {
        return readAllLogs().stream()
            .filter(log -> log.getLevel().equalsIgnoreCase(level))
            .toList();
    }
    
    private List<LogEntryDTO> readLogs(int maxEntries) {
        File logFile = new File(logFilePath);
        
        if (!logFile.exists()) {
            logger.warn("Log file not found: {}", logFilePath);
            return Collections.emptyList();
        }
        
        List<LogEntryDTO> logs = new ArrayList<>();
        
        try {
            List<String> allLines = Files.readAllLines(Paths.get(logFilePath));
  
            Collections.reverse(allLines);
            
            for (String line : allLines) {
                if (logs.size() >= maxEntries) {
                    break;
                }
                
                LogEntryDTO entry = parseLine(line);
                if (entry != null) {
                    logs.add(entry);
                }
            }
            
            Collections.reverse(logs);
            
            logger.debug("Read {} log entries from {}", logs.size(), logFilePath);
            
        } catch (IOException e) {
            logger.error("Error reading log file: {}", logFilePath, e);
        }
        
        return logs;
    }

    private LogEntryDTO parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        
        Matcher matcher = LOG_PATTERN.matcher(line);
        
        if (matcher.matches()) {
            try {
                String timestampStr = matcher.group(1);  
                String thread = matcher.group(2);        
                String level = matcher.group(3);         
                String source = matcher.group(4);        
                String message = matcher.group(5);       
                
                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, LOGBACK_FORMATTER);
            
                String simpleSource = source.contains(".") 
                    ? source.substring(source.lastIndexOf('.') + 1) 
                    : source;
                
                return new LogEntryDTO(level, message, timestamp, simpleSource);
                
            } catch (DateTimeParseException e) {
                logger.warn("Failed to parse timestamp in log line: {}", line);
            }
        } else {
            logger.trace("Line doesn't match log pattern (might be stack trace): {}", line);
        }
        
        return null;
    }
    
    public boolean isLogFileAvailable() {
        File logFile = new File(logFilePath);
        return logFile.exists() && logFile.canRead();
    }
    
    public long getLogFileSize() {
        File logFile = new File(logFilePath);
        return logFile.exists() ? logFile.length() : 0;
    }
    
    public long getLogFileLineCount() {
        try {
            return Files.lines(Paths.get(logFilePath)).count();
        } catch (IOException e) {
            logger.error("Error counting log file lines", e);
            return 0;
        }
    }
}