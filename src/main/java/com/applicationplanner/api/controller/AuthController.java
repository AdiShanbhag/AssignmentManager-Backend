package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.GoogleAuthRequest;
import com.applicationplanner.api.dto.responseDTO.AuthResponse;
import com.applicationplanner.api.service.auth.AuthService;
import com.applicationplanner.api.service.auth.GoogleTokenVerifier;
import com.applicationplanner.api.service.auth.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final GoogleTokenVerifier googleTokenVerifier;
    private final AuthService authService;
    private final JwtService jwtService;

    public AuthController(GoogleTokenVerifier googleTokenVerifier, AuthService authService, JwtService jwtService) {
        this.googleTokenVerifier = googleTokenVerifier;
        this.authService = authService;
        this.jwtService = jwtService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@RequestBody GoogleAuthRequest req) {
        var payload = googleTokenVerifier.verify(req.idToken());
        var user = authService.loginWithGoogle(payload);
        var token = jwtService.issueToken(user.getId());

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getDisplayName()
        ));
    }
}
