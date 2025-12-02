package com.serverPresentation.http.controllers;

import com.serverApplication.dto.LogEntryDTO;
import com.serverApplication.useCases.GetServerLogsService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LogsRestController {
    private static final Logger logger = LoggerFactory.getLogger(LogsRestController.class);
    private final GetServerLogsService getServerLogsService;

    public LogsRestController(GetServerLogsService getServerLogsService) {
        this.getServerLogsService = getServerLogsService;
    }

    public void getLogs(Context ctx) {
        try {
            String level = ctx.queryParam("level");
            String limitParam = ctx.queryParam("limit");
            
            List<LogEntryDTO> logs;
            
            if (level != null && !level.isBlank()) {
                logs = getServerLogsService.getLogsByLevel(level);
            } else if (limitParam != null) {
                int limit = Integer.parseInt(limitParam);
                logs = getServerLogsService.getRecentLogs(limit);
            } else {
                logs = getServerLogsService.getRecentLogs();
            }
            
            ctx.json(logs);
            logger.debug("Returned {} log entries", logs.size());
        } catch (NumberFormatException e) {
            logger.warn("Invalid limit parameter: {}", ctx.queryParam("limit"));
            ctx.status(400).json(new ErrorResponse("Invalid limit parameter"));
        } catch (Exception e) {
            logger.error("Error retrieving logs: {}", e.getMessage(), e);
            ctx.status(500).json(new ErrorResponse("Error retrieving logs"));
        }
    }

    private record ErrorResponse(String error) {}
}
