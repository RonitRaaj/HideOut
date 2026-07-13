package com.hideout.backend.service;

import com.hideout.backend.models.Logs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private final LogExtraction logExtractionService;
    private final RestClient restClient = RestClient.create();

    @Value("${GEMINI_API_KEY}")
    private String apiKey;

    public GeminiService(LogExtraction logExtractionService) {
        this.logExtractionService = logExtractionService;
    }

   
    public String getChatSummaryPipeline(String sessionId, int hours) {

        List<Logs> logs = logExtractionService.getLogsFromLastNHours(sessionId, hours);

        if (logs == null || logs.isEmpty()) {
            return "No conversations were recorded in this room during the last " + hours + " hours.";
        }

        StringBuilder transcriptBuilder = new StringBuilder();
        for (Logs log : logs) {
            transcriptBuilder.append(String.format("[%s] (%s): %s\n", 
                log.getCreatedAt().toLocalTime(), 
                log.getSourceDevice(),            
                log.getContent()                  
            ));
        }

        String promptInstruction = 
            "You are an AI assistant for a secure private messaging application named Hideout.\n\n" +
            "Your task is to summarize the following chat transcript recorded over the last " + hours + " hours.\n" +
            "Please outline the main topics discussed and key interaction dynamics based on their source devices.\n\n" +
            "Rules:\n" +
            "- Keep your response concise and structured.\n" +
            "- Format everything using clean Markdown (bullet points, bold highlights).\n\n" +
            "--- CHAT TRANSCRIPT START ---\n" + 
            transcriptBuilder.toString() + 
            "\n--- CHAT TRANSCRIPT END ---";

        Map<String, Object> requestPayload = Map.of(
            "contents", List.of(
                Map.of("parts", List.of(
                    Map.of("text", promptInstruction)
                ))
            )
        );

        try {
            String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=" + apiKey;

            Map<String, Object> apiResponse = restClient.post()
                .uri(apiUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestPayload)
                .retrieve()
                .body(Map.class);

            List<?> candidates = (List<?>) apiResponse.get("candidates");
            Map<?, ?> firstCandidate = (Map<?, ?>) candidates.get(0);
            Map<?, ?> contentNode = (Map<?, ?>) firstCandidate.get("content");
            List<?> partsList = (List<?>) contentNode.get("parts");
            Map<?, ?> textPart = (Map<?, ?>) partsList.get(0);

            return (String) textPart.get("text");

        } catch (Exception e) {
            e.printStackTrace();
            return "⚠️ Error generating AI summary: " + e.getMessage();
        }
    }
}