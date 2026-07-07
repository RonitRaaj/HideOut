package com.hideout.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hideout.backend.dTO.LogEntry;
import com.hideout.backend.dTO.LogResponse;
import com.hideout.backend.dTO.EnterSessionDTO;
import com.hideout.backend.models.LogType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final LogsService logsService;
    
    private final Map<String, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String room = (String) session.getAttributes().get("room");
        String pokemon = (String) session.getAttributes().get("pokemon");

        if (room == null || pokemon == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        String standardRoom = room.toUpperCase().trim();
        roomSessions.computeIfAbsent(standardRoom, k -> new CopyOnWriteArrayList<>()).add(session);

        LogEntry systemAlert = new LogEntry();
        systemAlert.setType(LogType.SYSTEM);
        systemAlert.setContent(pokemon + " joined the hideout.");

        EnterSessionDTO context = new EnterSessionDTO(standardRoom, pokemon);
        logsService.saveItem(systemAlert, context);

        broadcastToRoom(standardRoom, new LogResponse(
                null,
                systemAlert.getContent(),
                pokemon,
                LocalDateTime.now(),
                LogType.SYSTEM.name()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String room = (String) session.getAttributes().get("room");
        String pokemon = (String) session.getAttributes().get("pokemon");

        if (room == null || pokemon == null) return;
        String standardRoom = room.toUpperCase().trim();

        LogEntry incomingRequest = objectMapper.readValue(message.getPayload(), LogEntry.class);
        incomingRequest.setType(LogType.CHAT);

        // FIX: Replaced DeviceType.valueOf with our safe parser
        EnterSessionDTO context = new EnterSessionDTO(standardRoom, pokemon);
        logsService.saveItem(incomingRequest, context);

        broadcastToRoom(standardRoom, new LogResponse(
                null,
                incomingRequest.getContent(),
                pokemon,
                LocalDateTime.now(),
                LogType.CHAT.name()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String room = (String) session.getAttributes().get("room");
        String pokemon = (String) session.getAttributes().get("pokemon");

        if (room != null) {
            String standardRoom = room.toUpperCase().trim();
            if (roomSessions.containsKey(standardRoom)) {
                roomSessions.get(standardRoom).remove(session);

                LogEntry systemAlert = new LogEntry();
                systemAlert.setType(LogType.SYSTEM);
                systemAlert.setContent(pokemon + " left the hideout.");

                EnterSessionDTO context = new EnterSessionDTO(standardRoom, pokemon);
                logsService.saveItem(systemAlert, context);

                broadcastToRoom(standardRoom, new LogResponse(
                        null,
                        systemAlert.getContent(),
                        pokemon,
                        LocalDateTime.now(),
                        LogType.SYSTEM.name()
                ));
            }
        }
    }

    private void broadcastToRoom(String room, LogResponse payload) {
        if (room == null) return;
        List<WebSocketSession> sessions = roomSessions.get(room.toUpperCase().trim());
        if (sessions != null) {
            try {
                String jsonMessage = objectMapper.writeValueAsString(payload);
                TextMessage textMessage = new TextMessage(jsonMessage);

                for (WebSocketSession s : sessions) {
                    if (s.isOpen()) {
                        s.sendMessage(textMessage);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}