package com.serverInfrastructure.adapters;

import com.serverApplication.dto.LogEntryDTO;
import com.serverApplication.ports.LogReader;
import com.serverInfrastructure.logging.LogFileReader;

import java.util.List;


public class LogFileReaderAdapter implements LogReader {
    
    private final LogFileReader logFileReader;
    
    public LogFileReaderAdapter(String logFilePath) {
        this.logFileReader = new LogFileReader(logFilePath);
    }
    
    public LogFileReaderAdapter() {
        this.logFileReader = new LogFileReader();
    }
    
    @Override
    public List<LogEntryDTO> readAllLogs() {
        return logFileReader.readAllLogs();
    }
    
    @Override
    public List<LogEntryDTO> readRecentLogs(int maxEntries) {
        return logFileReader.readRecentLogs(maxEntries);
    }
    
    @Override
    public List<LogEntryDTO> readLogsByLevel(String level) {
        return logFileReader.readLogsByLevel(level);
    }
    
    @Override
    public boolean isAvailable() {
        return logFileReader.isLogFileAvailable();
    }
}
