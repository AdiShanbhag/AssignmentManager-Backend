package com.applicationplanner.api.auth;

import com.applicationplanner.api.dto.responseDTO.AuthResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Profile({"dev","test"})
@RestController
@RequestMapping("/auth/dev")
public class DevAuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    public DevAuthController(AuthService authService, JwtService jwtService) {
        this.authService = authService;
        this.jwtService = jwtService;
    }

    public record DevMintRequest(String email, String displayName) {}

    @PostMapping("/mint")
    public ResponseEntity<AuthResponse> mint(@RequestBody DevMintRequest req) {

        var email = (req.email() == null || req.email().isBlank())
                ? "dev.user@example.com"
                : req.email().trim();

        var displayName = (req.displayName() == null || req.displayName().isBlank())
                ? "Dev User"
                : req.displayName().trim();

        var user = authService.loginDev(email, displayName);
        var token = jwtService.issueToken(user.getId());

        return ResponseEntity.ok(new AuthResponse(
                token,
                user.getId(),
                user.getEmail(),
                user.getDisplayName()
        ));
    }
}