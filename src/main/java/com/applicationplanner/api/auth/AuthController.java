package com.applicationplanner.api.auth;

import com.applicationplanner.api.dto.requestDTO.GoogleAuthRequest;
import com.applicationplanner.api.dto.responseDTO.AuthResponse;
import com.applicationplanner.api.model.User;
import com.applicationplanner.api.record.GoogleUserPayload;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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

    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletResponse response
    ) throws IOException {
        //System.out.println("CALLBACK received - code: " + code + " error: " + error);

        if (error != null) {
            response.sendRedirect("com.aditya1374.assignmentmanager://auth?error=" + error);
            return;
        }

        if (code == null) {
            response.sendRedirect("com.aditya1374.assignmentmanager://auth?error=no_code");
            return;
        }

        try {
            GoogleUserPayload payload = googleTokenVerifier.exchangeCode(code);
            User user = authService.loginWithGoogle(payload);
            String jwt = jwtService.issueToken(user.getId());

            response.sendRedirect(
                    "com.aditya1374.assignmentmanager://auth?token=" + jwt
                            + "&userId=" + user.getId()
                            + "&email=" + URLEncoder.encode(user.getEmail(), StandardCharsets.UTF_8)
                            + "&displayName=" + URLEncoder.encode(user.getDisplayName() != null ? user.getDisplayName() : "", StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            System.out.println("CALLBACK ERROR: " + e.getMessage());
            e.printStackTrace();
            response.sendRedirect("com.aditya1374.assignmentmanager://auth?error=backend_failed");
        }
    }
}
