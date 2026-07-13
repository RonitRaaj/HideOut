package com.hideout.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hideout.backend.dTO.LogEntry;
import com.hideout.backend.dTO.LogResponse;
import com.hideout.backend.dTO.EnterSessionDTO;
import com.hideout.backend.models.LogType;
import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final LogsService logsService;

    // Active connection tracking
    private final Map<String, List<WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    // 👇 Centralized timestamp tracking to handle presence lazily
    private static final Map<String, Long> userLastSeen = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String room = (String) session.getAttributes().get("room");
        String pokemon = (String) session.getAttributes().get("pokemon");

        if (room != null && pokemon != null) {
            String standardRoom = room.toUpperCase().trim();
            String uniqueUserKey = standardRoom + ":" + pokemon.toLowerCase().trim();

            // 1. Check if they are already considered active in our tracking registry
            boolean isBrandNewUser = !userLastSeen.containsKey(uniqueUserKey);
            
            // 2. Mark them active immediately with the current system time
            userLastSeen.put(uniqueUserKey, System.currentTimeMillis());

            // 3. Add their network session pointer to the room
            roomSessions.computeIfAbsent(standardRoom, k -> new CopyOnWriteArrayList<>()).add(session);

            // 4. Only broadcast and save a JOIN message if they are completely new to the system
            if (isBrandNewUser) {
                LogEntry systemAlert = new LogEntry();
                systemAlert.setType(LogType.SYSTEM);
                systemAlert.setContent("[System] " + pokemon + " joined the hideout.");

                EnterSessionDTO context = new EnterSessionDTO(standardRoom, pokemon.trim());
                logsService.saveItem(systemAlert, context);

                broadcastToRoom(standardRoom, new LogResponse(
                        null,
                        systemAlert.getContent(),
                        pokemon,
                        LocalDateTime.now(),
                        LogType.SYSTEM.name()));
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String room = (String) session.getAttributes().get("room");
        String pokemon = (String) session.getAttributes().get("pokemon");

        if (room == null || pokemon == null)
            return;
        String standardRoom = room.toUpperCase().trim();

        LogEntry incomingRequest = objectMapper.readValue(message.getPayload(), LogEntry.class);
        incomingRequest.setType(LogType.CHAT);

        EnterSessionDTO context = new EnterSessionDTO(standardRoom, pokemon);
        logsService.saveItem(incomingRequest, context);

        broadcastToRoom(standardRoom, new LogResponse(
                null,
                incomingRequest.getContent(),
                pokemon,
                LocalDateTime.now(),
                LogType.CHAT.name()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String room = (String) session.getAttributes().get("room");
        if (room != null) {
            String standardRoom = room.toUpperCase().trim();
            List<WebSocketSession> sessions = roomSessions.get(standardRoom);
            if (sessions != null) {
                // 👇 CLEANUP: Simply drop the raw network handle. 
                // Do NOT broadcast a "Left" message here—let the lazy sweeper handle it!
                sessions.remove(session);
            }
        }
    }

    // 🔁 Runs every 10 seconds globally to sweep away dead sessions
    @Scheduled(fixedRate = 10000)
    public void verifyUserPresence() {
        long now = System.currentTimeMillis();

        // Step A: Keep timestamps fresh for everyone who has an OPEN, active browser tab
        roomSessions.forEach((roomCode, sessions) -> {
            for (WebSocketSession session : sessions) {
                String pokemon = (String) session.getAttributes().get("pokemon");
                if (pokemon != null && session.isOpen()) {
                    String uniqueUserKey = roomCode + ":" + pokemon.toLowerCase().trim();
                    userLastSeen.put(uniqueUserKey, now); // Update last seen timestamp
                }
            }
        });

        // Step B: Sweep out users who have dropped offline and exceeded the 15-second grace period
        userLastSeen.forEach((uniqueUserKey, lastSeenTime) -> {
            if (now - lastSeenTime > 15000) { // Missing for more than 15 seconds
                userLastSeen.remove(uniqueUserKey);

                // Parse out values from "ROOMCODE:pokemonname"
                String[] parts = uniqueUserKey.split(":");
                if (parts.length == 2) {
                    String roomCode = parts[0];
                    String rawPokemonName = parts[1];

                    // Capitalize first letter beautifully for the system message display
                    String formattedName = rawPokemonName.substring(0, 1).toUpperCase() + rawPokemonName.substring(1);

                    LogEntry systemAlert = new LogEntry();
                    systemAlert.setType(LogType.SYSTEM);
                    systemAlert.setContent("[System] " + formattedName + " left the hideout.");

                    try {
                        EnterSessionDTO context = new EnterSessionDTO(roomCode, formattedName);
                        logsService.saveItem(systemAlert, context);

                        broadcastToRoom(roomCode, new LogResponse(
                                null,
                                systemAlert.getContent(),
                                formattedName,
                                LocalDateTime.now(),
                                LogType.SYSTEM.name()));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void broadcastToRoom(String room, LogResponse payload) {
        if (room == null)
            return;
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

    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void cleanAbandonedRooms() {
        roomSessions.forEach((roomCode, sessionsList) -> {
            if (sessionsList.isEmpty()) {
                roomSessions.remove(roomCode);
            }
        });
    }
}