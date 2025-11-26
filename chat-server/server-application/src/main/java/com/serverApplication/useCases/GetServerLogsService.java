package com.serverApplication.useCases;

import com.serverApplication.dto.LogEntryDTO;
import com.serverApplication.ports.LogReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class GetServerLogsService {
    private static final Logger logger = LoggerFactory.getLogger(GetServerLogsService.class);
    private static final int DEFAULT_MAX_LOGS = 1000;
    
    private final LogReader logReader;
    
    public GetServerLogsService(LogReader logReader) {
        this.logReader = logReader;
    }

    public List<LogEntryDTO> getRecentLogs() {
        logger.debug("Fetching recent server logs");
        return logReader.readRecentLogs(DEFAULT_MAX_LOGS);
    }
    
    public List<LogEntryDTO> getAllLogs() {
        logger.debug("Fetching all server logs");
        return logReader.readAllLogs();
    }
    
    public List<LogEntryDTO> getLogsByLevel(String level) {
        logger.debug("Fetching logs with level: {}", level);
        return logReader.readLogsByLevel(level);
    }
    
    public List<LogEntryDTO> getRecentLogs(int maxEntries) {
        logger.debug("Fetching {} recent logs", maxEntries);
        return logReader.readRecentLogs(maxEntries);
    }
}