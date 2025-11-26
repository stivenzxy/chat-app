package com.serverApplication.ports;

import com.serverApplication.dto.LogEntryDTO;
import java.util.List;

public interface LogReader {
    List<LogEntryDTO> readAllLogs();
    List<LogEntryDTO> readRecentLogs(int maxEntries);
    List<LogEntryDTO> readLogsByLevel(String level);
    boolean isAvailable();
}
