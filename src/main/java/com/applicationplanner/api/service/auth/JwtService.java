package com.applicationplanner.api.service.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import io.jsonwebtoken.io.DecodingException;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.auth.jwt.secret}") String secret,
            @Value("${app.auth.jwt.ttl-seconds}") long ttlSeconds
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET is not set");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (DecodingException e) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String issueToken(UUID userId) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public UUID parseUserId(String jwt) {
        String sub = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(jwt)
                .getPayload()
                .getSubject();

        return UUID.fromString(sub);
    }
}