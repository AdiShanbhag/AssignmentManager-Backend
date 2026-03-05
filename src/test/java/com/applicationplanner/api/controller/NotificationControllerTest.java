package com.applicationplanner.api.controller;

import com.applicationplanner.api.model.DeviceToken;
import com.applicationplanner.api.repository.DeviceTokenRepository;
import com.applicationplanner.api.util.WithMockUserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceTokenRepository deviceTokenRepository;

    /**
     * Sets up security context before each test. By Claude
     */
    @BeforeEach
    void setup() {
        WithMockUserId.set();
    }

    /**
     * Clears security context after each test. By Claude
     */
    @AfterEach
    void teardown() {
        WithMockUserId.clear();
    }

    /**
     * Verifies that registering a new token returns 201 Created and saves to DB. By Claude
     */
    @Test
    void registerToken_newToken_returns201() throws Exception {
        when(deviceTokenRepository.findByToken("new-fcm-token"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/notifications/register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "new-fcm-token",
                                    "platform": "android"
                                }
                                """))
                .andExpect(status().isCreated());

        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    /**
     * Verifies that registering an existing token returns 200 OK and does not save again. By Claude
     */
    @Test
    void registerToken_existingToken_returns200AndDoesNotSave() throws Exception {
        DeviceToken existing = DeviceToken.builder()
                .token("existing-fcm-token")
                .platform("android")
                .userId(WithMockUserId.TEST_USER_ID)
                .build();

        when(deviceTokenRepository.findByToken("existing-fcm-token"))
                .thenReturn(Optional.of(existing));

        mockMvc.perform(post("/notifications/register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "existing-fcm-token",
                                    "platform": "android"
                                }
                                """))
                .andExpect(status().isOk());

        verify(deviceTokenRepository, never()).save(any(DeviceToken.class));
    }

    /**
     * Verifies that registering a token with missing fields returns 400. By Claude
     */
    @Test
    void registerToken_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/notifications/register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "platform": "android"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that unregistering a token returns 204 No Content. By Claude
     */
    @Test
    void unregisterToken_validToken_returns204() throws Exception {
        mockMvc.perform(delete("/notifications/unregister-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "token": "fcm-token-to-remove"
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(deviceTokenRepository).deleteByToken(eq("fcm-token-to-remove"));
    }

    /**
     * Verifies that unregistering with missing token returns 400. By Claude
     */
    @Test
    void unregisterToken_missingToken_returns400() throws Exception {
        mockMvc.perform(delete("/notifications/unregister-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that registering an existing token for a different user reassigns it. By Claude
     */
    @Test
    void registerToken_existingTokenDifferentUser_reassignsToken() throws Exception {
        UUID differentUserId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        DeviceToken existingToken = DeviceToken.builder()
                .token("shared-device-token")
                .platform("android")
                .userId(differentUserId) // belongs to a different user
                .build();

        when(deviceTokenRepository.findByToken("shared-device-token"))
                .thenReturn(Optional.of(existingToken));

        mockMvc.perform(post("/notifications/register-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "token": "shared-device-token",
                                "platform": "android"
                            }
                            """))
                .andExpect(status().isOk());

        // verify token was saved with the new user id
        verify(deviceTokenRepository).save(argThat(token ->
                token.getUserId().equals(WithMockUserId.TEST_USER_ID)
        ));
    }
}