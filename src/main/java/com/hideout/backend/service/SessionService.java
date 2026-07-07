package com.hideout.backend.service;

import com.hideout.backend.dTO.LogResponse;
import com.hideout.backend.models.Session;
import com.hideout.backend.repository.SessionLogsRepository;
import com.hideout.backend.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionLogsRepository sessionLogsRepository;

    @Transactional
    public Session createSession() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (sessionRepository.existsById(code));

        Session session = new Session();
        session.setSessionId(code);
        sessionRepository.save(session);

        return session;
    }

    @Transactional(readOnly = true)
    public List<LogResponse> getSessionHistory(String sessionId) {
        return sessionLogsRepository.findBySessionSessionIdOrderByLogCreatedAtAsc(sessionId)
            .stream()
            .map(item -> new LogResponse(
                    item.getLog().getId(),
                    item.getLog().getContent(),
                    item.getLog().getSourceDevice(),
                    item.getLog().getCreatedAt(),
                    item.getLog().getLogType().name()
            ))
            .collect(Collectors.toList());
    }
}