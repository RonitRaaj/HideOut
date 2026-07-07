package com.hideout.backend.controller;

import com.hideout.backend.service.SessionService;
import lombok.RequiredArgsConstructor;
import com.hideout.backend.models.Session;
import com.hideout.backend.dTO.LogResponse;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/create")
    public ResponseEntity<Session> createSession() {
        Session session = sessionService.createSession();
        return ResponseEntity.ok(session);
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<LogResponse>> getSessionHistory(@PathVariable String sessionId) {
        List<LogResponse> logResponses = sessionService.getSessionHistory(sessionId);
        return ResponseEntity.ok(logResponses);
    }
}