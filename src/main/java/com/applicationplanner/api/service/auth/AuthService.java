package com.applicationplanner.api.service.auth;

import com.applicationplanner.api.enums.AuthProvider;
import com.applicationplanner.api.model.User;
import com.applicationplanner.api.model.UserIdentity;
import com.applicationplanner.api.record.GoogleUserPayload;
import com.applicationplanner.api.repository.UserIdentityRepository;
import com.applicationplanner.api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;

    public AuthService(UserRepository userRepository, UserIdentityRepository identityRepository) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
    }

    @Transactional
    public User loginWithGoogle(GoogleUserPayload payload) {
        var existing = identityRepository
                .findByProviderAndProviderSubject(AuthProvider.GOOGLE, payload.subject());

        if (existing.isPresent()) {
            // Optional: update email/display name if changed
            var user = existing.get().getUser();
            if (payload.email() != null && !payload.email().equals(user.getEmail())) user.setEmail(payload.email());
            if (payload.name() != null && !payload.name().equals(user.getDisplayName())) user.setDisplayName(payload.name());
            return userRepository.save(user);
        }

        var now = OffsetDateTime.now();
        var user = User.builder()
                .email(payload.email())
                .displayName(payload.name())
                .createdAt(now)
                .updatedAt(now)
                .build();

        user = userRepository.save(user);

        var identity = UserIdentity.builder()
                .user(user)
                .provider(AuthProvider.GOOGLE)
                .providerSubject(payload.subject())
                .email(payload.email())
                .build();

        identityRepository.save(identity);

        return user;
    }
}