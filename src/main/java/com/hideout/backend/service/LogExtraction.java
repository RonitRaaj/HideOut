package com.hideout.backend.service;

import com.hideout.backend.models.Logs;
import com.hideout.backend.repository.LogsRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LogExtraction {

    private final LogsRepository logsRepository;

    public LogExtraction(LogsRepository logsRepository) {
        this.logsRepository = logsRepository;
    }

    /**
     * Extracts chat logs for a specific session within a historical window.
     * 
     * @param sessionId The 6-character room code.
     * @param hours The size of the window to look back.
     * @return A list of Logs entities sorted oldest to newest.
     */
    public List<Logs> getLogsFromLastNHours(String sessionId, int hours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hours);
        
        List<Logs> extractedLogs = logsRepository.findLogsBySessionIdAndTimeWindow(sessionId, cutoffTime);
        
        System.out.println("Extracted " + extractedLogs.size() + " messages for session [" + sessionId + "] from the last " + hours + " hours.");
        
        return extractedLogs;
    }
}