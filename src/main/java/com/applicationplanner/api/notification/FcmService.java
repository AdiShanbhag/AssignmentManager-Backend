package com.applicationplanner.api.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Slf4j
@Service
public class FcmService {

    private final GoogleCredentials googleCredentials;
    private final ObjectMapper objectMapper;
    private final String projectId;
    private final HttpClient httpClient;

    public FcmService(
            GoogleCredentials googleCredentials,
            ObjectMapper objectMapper,
            @Value("${app.firebase.project-id}") String projectId
    ) {
        this.googleCredentials = googleCredentials;
        this.objectMapper = objectMapper;
        this.projectId = projectId;
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Sends a push notification to a single FCM device token. By Claude
     */
    public void sendNotification(String deviceToken, String title, String body) {
        try {
            String accessToken = getAccessToken();

            Map<String, Object> message = Map.of(
                    "message", Map.of(
                            "token", deviceToken,
                            "notification", Map.of(
                                    "title", title,
                                    "body", body
                            )
                    )
            );

            String payload = objectMapper.writeValueAsString(message);
            String url = "https://fcm.googleapis.com/v1/projects/" + projectId + "/messages:send";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("FCM notification sent successfully to token: {}", deviceToken);
            } else {
                log.warn("FCM notification failed. Status: {}, Body: {}", response.statusCode(), response.body());
            }

        } catch (Exception e) {
            log.error("Failed to send FCM notification to token: {}", deviceToken, e);
        }
    }

    /**
     * Refreshes and returns a valid OAuth2 access token for FCM. By Claude
     */
    private String getAccessToken() throws IOException {
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }
}