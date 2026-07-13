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
                    log.getContent()));
        }

        String promptInstruction = "You are an AI assistant for a secure private messaging application named Hideout.\n\n"
                + "Your task is to summarize the following chat transcript recorded over the last " + hours
                + " hours.\n\n" +
                "Rules:\n" +
                "- Be extremely brief, punchy, and sweet.\n" +
                "- Add a touch of playful, clever sass about what went down (or didn't go down).\n" +
                "- Use light Markdown formatting (bullet points, bold headers).\n" +
                "- Do not write an essay. Keep the entire response under 3-4 bullet points max.\n\n" +
                "--- CHAT TRANSCRIPT START ---\n" +
                transcriptBuilder.toString() +
                "\n--- CHAT TRANSCRIPT END ---";

        Map<String, Object> requestPayload = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", promptInstruction)))));

        // 👇 UPDATED: Active production endpoints for resilient fallbacks
        String[] models = {"gemini-3.5-flash", "gemini-3.1-flash-lite", "gemini-2.5-flash"};
        String apiResponseText = null;
        Exception lastException = null;

        for (String model : models) {
            try {
                String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

                System.out.println("Attempting AI summary with model: " + model);

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

                apiResponseText = (String) textPart.get("text");
                
                // Break immediately if a valid response is achieved
                if (apiResponseText != null) {
                    break; 
                }

            } catch (Exception e) {
                System.out.println("⚠️ Model " + model + " failed or overloaded. Trying next fallback...");
                lastException = e; 
            }
        }

        // Return parsed text or custom demand boundary message
        if (apiResponseText != null) {
            return apiResponseText;
        } else {
            if (lastException != null) {
                lastException.printStackTrace();
            }
            return "⏰ The AI frequencies are totally jammed right now. Google is facing high demand. I am using free tier yk, Money issues. please try again in a few minutes, or don't. your wish ";
        }
    }
}