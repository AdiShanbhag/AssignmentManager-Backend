package com.applicationplanner.api.repository;

import com.applicationplanner.api.model.DeviceToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

// By Claude - Repository for device token CRUD operations (Story 11.2)
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    /**
     * Finds a device token by its FCM token string. By Claude
     */
    Optional<DeviceToken> findByToken(String token);

    /**
     * Deletes a device token by its FCM token string. By Claude
     */
    @Transactional
    void deleteByToken(String token);
}