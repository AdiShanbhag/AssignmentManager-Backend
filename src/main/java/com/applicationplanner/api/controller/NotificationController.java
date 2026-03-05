package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.RegisterTokenRequest;
import com.applicationplanner.api.dto.requestDTO.UnregisterTokenRequest;
import com.applicationplanner.api.model.DeviceToken;
import com.applicationplanner.api.repository.DeviceTokenRepository;
import com.applicationplanner.api.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

// By Claude - Handles device token registration and unregistration for push notifications (Story 11.2)
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final DeviceTokenRepository deviceTokenRepository;

    public NotificationController(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Registers a device token for the current user.
     * If the token exists for a different user, reassigns it to the current user. By Claude
     */
    @PostMapping("/register-token")
    public ResponseEntity<Void> registerToken(@Valid @RequestBody RegisterTokenRequest req) {
        UUID userId = CurrentUser.requireUserId();

        Optional<DeviceToken> existing = deviceTokenRepository.findByToken(req.token());

        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            if (token.getUserId().equals(userId)) {
                // Same user, same token — nothing to do
                return ResponseEntity.ok().build();
            }
            // Different user on same device — reassign token
            token.setUserId(userId);
            deviceTokenRepository.save(token);
            return ResponseEntity.ok().build();
        }

        DeviceToken deviceToken = DeviceToken.builder()
                .userId(userId)
                .token(req.token())
                .platform(req.platform())
                .build();

        deviceTokenRepository.save(deviceToken);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Unregisters a device token on logout. By Claude
     */
    @DeleteMapping("/unregister-token")
    public ResponseEntity<Void> unregisterToken(@Valid @RequestBody UnregisterTokenRequest req) {
        deviceTokenRepository.deleteByToken(req.token());
        return ResponseEntity.noContent().build();
    }
}