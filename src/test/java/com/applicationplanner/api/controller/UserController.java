package com.applicationplanner.api.controller;

import com.applicationplanner.api.model.User;
import com.applicationplanner.api.repository.UserRepository;
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

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    private User testUser;

    /**
     * Sets up a test user with default notification preferences before each test. By Claude
     */
    @BeforeEach
    void setup() {
        WithMockUserId.set();
        testUser = User.builder()
                .id(WithMockUserId.TEST_USER_ID)
                .email("test@test.com")
                .displayName("Test User")
                .timezone("UTC")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(userRepository.findById(WithMockUserId.TEST_USER_ID))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    /**
     * Clears security context after each test. By Claude
     */
    @AfterEach
    void teardown() {
        WithMockUserId.clear();
    }

    /**
     * Verifies GET /users/me returns all fields including notification preferences. By Claude
     */
    @Test
    void getMe_returnsFullProfile_includingNotificationPreferences() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andExpect(jsonPath("$.notificationsEnabled").value(true))
                .andExpect(jsonPath("$.dailyReminderEnabled").value(true))
                .andExpect(jsonPath("$.dailyReminderTime").value("08:00"))
                .andExpect(jsonPath("$.dueDateWarningEnabled").value(true))
                .andExpect(jsonPath("$.dueDateWarningDaysBefore").value(2))
                .andExpect(jsonPath("$.atRiskAlertEnabled").value(true));
    }

    /**
     * Verifies PATCH /users/me updates notification preferences correctly. By Claude
     */
    @Test
    void updateMe_updatesNotificationPreferences() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "notificationsEnabled": false,
                                    "dailyReminderTime": "09:30",
                                    "dueDateWarningDaysBefore": 5,
                                    "atRiskAlertEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notificationsEnabled").value(false))
                .andExpect(jsonPath("$.dailyReminderTime").value("09:30"))
                .andExpect(jsonPath("$.dueDateWarningDaysBefore").value(5))
                .andExpect(jsonPath("$.atRiskAlertEnabled").value(false));

        verify(userRepository).save(any(User.class));
    }

    /**
     * Verifies PATCH /users/me does not modify fields that are not included in the request. By Claude
     */
    @Test
    void updateMe_omittedFieldsRemainUnchanged() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "notificationsEnabled": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyReminderEnabled").value(true))
                .andExpect(jsonPath("$.dailyReminderTime").value("08:00"))
                .andExpect(jsonPath("$.dueDateWarningEnabled").value(true))
                .andExpect(jsonPath("$.dueDateWarningDaysBefore").value(2))
                .andExpect(jsonPath("$.atRiskAlertEnabled").value(true));
    }

    /**
     * Verifies PATCH /users/me rejects invalid dailyReminderTime format. By Claude
     */
    @Test
    void updateMe_invalidDailyReminderTime_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "dailyReminderTime": "9:30am"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies PATCH /users/me rejects dueDateWarningDaysBefore outside valid range. By Claude
     */
    @Test
    void updateMe_invalidDueDateWarningDays_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "dueDateWarningDaysBefore": 0
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies PATCH /users/me rejects invalid timezone. By Claude
     */
    @Test
    void updateMe_invalidTimezone_returnsBadRequest() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "timezone": "Bad/Zone"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}