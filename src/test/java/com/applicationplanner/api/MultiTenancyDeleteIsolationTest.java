package com.applicationplanner.api;

import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.User;
import com.applicationplanner.api.repository.AssignmentRepository;
import com.applicationplanner.api.repository.UserRepository;
import com.applicationplanner.api.service.auth.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MultiTenancyDeleteIsolationTest {

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AssignmentRepository assignmentRepository;
    @Autowired JwtService jwtService;

    @Test
    void delete_is_scoped_to_owner_user() throws Exception {

        // --- Arrange: create two users ---
        User userA = new User();
        userA.setEmail("a@test.com");
        userA.setDisplayName("User A");
        userA.setCreatedAt(OffsetDateTime.now());
        userA.setUpdatedAt(OffsetDateTime.now());
        userA = userRepository.saveAndFlush(userA);

        User userB = new User();
        userB.setEmail("b@test.com");
        userB.setDisplayName("User B");
        userB.setCreatedAt(OffsetDateTime.now());
        userB.setUpdatedAt(OffsetDateTime.now());
        userB = userRepository.saveAndFlush(userB);

        // --- Assignment belongs to user A ---
        Assignment a = new Assignment();
        a.setUserId(userA.getId());
        a.setTitle("A's Assignment");
        a.setSubject("Test");
        a.setDueDate(LocalDate.parse("2026-03-01"));
        a.setPlanningDays(5);
        a = assignmentRepository.saveAndFlush(a);

        String tokenA = jwtService.issueToken(userA.getId());
        String tokenB = jwtService.issueToken(userB.getId());

        // --- Act: user B attempts to delete A’s assignment ---
        mockMvc.perform(delete("/assignments/{id}", a.getId())
                        .param("today", "2026-02-22")
                        .header("Authorization", "Bearer " + tokenB)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // --- Assert: assignment still exists for user A ---
        mockMvc.perform(get("/plan")
                        .param("today", "2026-02-22")
                        .header("Authorization", "Bearer " + tokenA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }
}