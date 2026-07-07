package com.hideout.backend.service;

import com.hideout.backend.dTO.LogEntry;
import com.hideout.backend.dTO.EnterSessionDTO;
import com.hideout.backend.models.Logs;
import com.hideout.backend.models.Session;
import com.hideout.backend.models.SessionLogs;
import com.hideout.backend.repository.*;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogsService {

    private final LogsRepository clipboardItemRepository;
    private final SessionRepository sessionRepository;
    private final SessionLogsRepository sessionLogsRepository;

    @Transactional
    public void saveItem(LogEntry request, EnterSessionDTO sessionContext) {

        Session activeSession = sessionRepository.findById(sessionContext.getSessionId().toUpperCase())
            .orElseThrow(() -> new IllegalArgumentException("Room code does not exist."));

        
        Logs item = new Logs();
        item.setContent(request.getContent());
        item.setSourceDevice(sessionContext.getDeviceType());
        item.setLogType(request.getType());
        clipboardItemRepository.save(item);

        SessionLogs sessionLogs = new SessionLogs();
        sessionLogs.setSession(activeSession);
        sessionLogs.setLog(item);

        sessionLogsRepository.save(sessionLogs);
    }

}