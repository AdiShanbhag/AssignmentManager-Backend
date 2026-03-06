package com.applicationplanner.api.config;

import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${app.firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${app.firebase.service-account-path:}")
    private String serviceAccountPath;

    /**
     * Creates a GoogleCredentials bean from either the service account JSON env var
     * or the service account file path, depending on the active profile. By Claude
     */
    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try
        {
            if (serviceAccountJson != null && !serviceAccountJson.isBlank()) {
                // Production - read from environment variable
                InputStream stream = new ByteArrayInputStream(
                        serviceAccountJson.getBytes(StandardCharsets.UTF_8)
                );
                return GoogleCredentials
                        .fromStream(stream)
                        .createScoped("https://www.googleapis.com/auth/firebase.messaging");
            }

            if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
                // Local dev - read from file
                return GoogleCredentials
                        .fromStream(new FileInputStream(serviceAccountPath))
                        .createScoped("https://www.googleapis.com/auth/firebase.messaging");
            }

            // By Claude - Return empty credentials for test profile where Firebase is not needed
            return GoogleCredentials.newBuilder().build();
        /*
        throw new IllegalStateException(
                "Firebase credentials not configured. " +
                        "Set app.firebase.service-account-json or app.firebase.service-account-path"
        );*/
        }
        catch (IOException ie)
        {
            throw new IllegalStateException("Firebase credentials not configured. " +
                    "Set app.firebase.service-account-json or app.firebase.service-account-path", ie);
        }
    }
}