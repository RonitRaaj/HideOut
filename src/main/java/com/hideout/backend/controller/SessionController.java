package com.hideout.backend.controller;

import com.hideout.backend.service.SessionService;
import com.hideout.backend.service.GeminiService;
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
    private final GeminiService geminiService;

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

    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<String> getChatSummary(
            @PathVariable String sessionId,
            @RequestParam(defaultValue = "1") int hours) {
        
        System.out.println("HTTP Request received: Summarize session [" + sessionId + "] for the last " + hours + " hours.");

        if (hours <= 0) {
            return ResponseEntity.badRequest().body("Hours parameter must be greater than 0.");
        }

        String summaryMarkdown = geminiService.getChatSummaryPipeline(sessionId, hours);

        return ResponseEntity.ok(summaryMarkdown);
    }
}