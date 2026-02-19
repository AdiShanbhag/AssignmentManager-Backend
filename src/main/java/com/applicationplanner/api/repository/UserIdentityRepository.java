package com.applicationplanner.api.repository;

import com.applicationplanner.api.enums.AuthProvider;
import com.applicationplanner.api.model.UserIdentity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepository extends JpaRepository<UserIdentity, UUID> {
    Optional<UserIdentity> findByProviderAndProviderSubject(AuthProvider provider, String providerSubject);
}