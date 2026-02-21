package com.applicationplanner.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthConfigSanity {

    @Bean
    ApplicationRunner authConfigCheck(
            @Value("${app.auth.google.client-ids}") String clientIds,
            @Value("${app.auth.jwt.secret}") String jwtSecret
    ) {
        return args -> {
            System.out.println("[AuthConfig] google client ids present: " + (!clientIds.isBlank()));
            System.out.println("[AuthConfig] jwt secret length: " + jwtSecret.length());
        };
    }
}