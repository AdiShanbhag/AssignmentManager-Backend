package com.applicationplanner.api;

import com.applicationplanner.api.enums.AuthProvider;
import com.applicationplanner.api.model.User;
import com.applicationplanner.api.model.UserIdentity;
import com.applicationplanner.api.repository.UserIdentityRepository;
import com.applicationplanner.api.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UserIdentityPersistenceTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Test
    @Transactional
    void canCreateUserAndFindIdentityByProviderAndSubject() {
        // create user
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        user.setDisplayName("Test User");
        user = userRepository.save(user);

        // link google identity
        UserIdentity identity = new UserIdentity();
        identity.setId(UUID.randomUUID());
        identity.setUser(user);
        identity.setProvider(AuthProvider.GOOGLE);
        identity.setProviderSubject("google-sub-123");
        identity.setEmail("test@example.com");
        userIdentityRepository.save(identity);

        // fetch back via provider + subject (this is the backbone of OAuth login)
        UserIdentity found = userIdentityRepository
                .findByProviderAndProviderSubject(AuthProvider.GOOGLE, "google-sub-123")
                .orElseThrow(() -> new AssertionError("Expected identity to be found"));

        assertEquals(user.getId(), found.getUser().getId());
        assertEquals(AuthProvider.GOOGLE, found.getProvider());
        assertEquals("google-sub-123", found.getProviderSubject());
        assertEquals("test@example.com", found.getEmail());
    }

    @Test
    void providerAndSubjectMustBeUnique() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("dupe@example.com");
        user.setDisplayName("Dupe");
        user = userRepository.save(user);

        UserIdentity a = new UserIdentity();
        a.setId(UUID.randomUUID());
        a.setUser(user);
        a.setProvider(AuthProvider.GOOGLE);
        a.setProviderSubject("same-sub");
        userIdentityRepository.save(a);

        UserIdentity b = new UserIdentity();
        b.setId(UUID.randomUUID());
        b.setUser(user);
        b.setProvider(AuthProvider.GOOGLE);
        b.setProviderSubject("same-sub");

        assertThrows(Exception.class, () -> {
            userIdentityRepository.saveAndFlush(b);
        });
    }
}