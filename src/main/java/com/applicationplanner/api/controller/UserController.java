package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.UpdateProfileRequest;
import com.applicationplanner.api.dto.responseDTO.UserProfileResponse;
import com.applicationplanner.api.model.User;
import com.applicationplanner.api.repository.UserRepository;
import com.applicationplanner.api.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the current authenticated user's profile. by Claude
     */
    @GetMapping("/me")
    public UserProfileResponse getMe() {
        UUID userId = CurrentUser.requireUserId();
        User user = requireUser(userId);
        return toResponse(user);
    }

    /**
     * Updates the current authenticated user's profile fields.
     * Only non-null fields in the request body are updated. by Claude
     */
    @PatchMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest req) {
        UUID userId = CurrentUser.requireUserId();
        User user = requireUser(userId);

        if (req.displayName() != null) {
            String name = req.displayName().trim();
            if (name.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "displayName cannot be blank");
            user.setDisplayName(name);
        }

        if (req.phoneNumber() != null) {
            user.setPhoneNumber(req.phoneNumber().trim());
        }

        if (req.university() != null) {
            user.setUniversity(req.university().trim());
        }

        if (req.timezone() != null) {
            String tz = req.timezone().trim();
            try {
                ZoneId.of(tz); // validate it's a real timezone
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid timezone: " + tz);
            }
            user.setTimezone(tz);
        }

        userRepository.save(user);
        return toResponse(user);
    }

    /**
     * Converts a User entity to a UserProfileResponse DTO. by Claude
     */
    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.getUniversity(),
                user.getTimezone(),
                user.getCreatedAt()
        );
    }

    /**
     * Fetches the user by ID or throws 404. by Claude
     */
    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}