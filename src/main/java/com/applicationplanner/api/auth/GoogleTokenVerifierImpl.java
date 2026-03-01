package com.applicationplanner.api.auth;

import com.applicationplanner.api.record.GoogleUserPayload;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;

@Component
public class GoogleTokenVerifierImpl implements GoogleTokenVerifier {

    private final GoogleIdTokenVerifier verifier;
    private String webClientId;
    private String clientSecret;
    private String clientId;
    private String androidClientId;
    private final String redirectUri;

    public GoogleTokenVerifierImpl(
            @Value("${app.auth.google.client-ids}") String clientIds,
            @Value("${app.auth.google.client-secret}") String clientSecret,
            @Value("${app.auth.google.android-client-id}") String androidClientId,
            @Value("${app.auth.google.web-client-id}") String webClientId,
            @Value("${app.auth.google.redirect-uri}") String redirectUri
    ) {

        this.clientSecret = clientSecret;
        this.androidClientId = androidClientId;;
        this.clientId = clientIds.split(",")[0].trim();
        this.webClientId = webClientId;
        this.redirectUri = redirectUri;

        List<String> audiences = Arrays.stream(clientIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();

        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance()
        )
                .setAudience(audiences)
                .build();
    }

    @Override
    public GoogleUserPayload verify(String idToken) {
        try {
            GoogleIdToken token = verifier.verify(idToken);
            if (token == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            var payload = token.getPayload();
            return new GoogleUserPayload(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name")
            );

        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException("Failed to verify Google ID token", e);
        }
    }


    @Override
    public GoogleUserPayload exchangeCode(String code) {
        //System.out.println("EXCHANGING CODE with redirectUri: " + redirectUri);
        //System.out.println("Using clientId: " + clientId);
        try {
            GoogleTokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    JacksonFactory.getDefaultInstance(),
                    webClientId,
                    clientSecret,
                    code,
                    redirectUri
            ).execute();

            GoogleIdToken idToken = tokenResponse.parseIdToken();
            GoogleIdToken.Payload payload = idToken.getPayload();

            return new GoogleUserPayload(
                    payload.getSubject(),
                    payload.getEmail(),
                    (String) payload.get("name")
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange Google code", e);
        }
    }
}