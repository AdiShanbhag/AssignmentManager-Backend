package com.applicationplanner.api.controller;

import com.applicationplanner.api.service.PlanningOrchestratorService;
import com.applicationplanner.api.util.WithMockUserId;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlanningOrchestratorService orchestrator;

    private final UUID assignmentId = UUID.randomUUID();
    private final UUID taskId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        WithMockUserId.set();
    }

    @AfterEach
    void teardown() {
        WithMockUserId.clear();
    }

    // --- toggle ---

    @Test
    void toggle_withValidTz_usesCorrectToday() throws Exception {
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(patch("/tasks/" + taskId + "/toggle")
                        .param("assignmentId", assignmentId.toString())
                        .param("tz", tz))
                .andExpect(status().isOk());

        verify(orchestrator).toggleTaskDoneAndPlan(eq(assignmentId), eq(taskId), eq(expected));
    }

    @Test
    void toggle_withNoTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(patch("/tasks/" + taskId + "/toggle")
                        .param("assignmentId", assignmentId.toString()))
                .andExpect(status().isOk());

        verify(orchestrator).toggleTaskDoneAndPlan(eq(assignmentId), eq(taskId), eq(expected));
    }

    @Test
    void toggle_withInvalidTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(patch("/tasks/" + taskId + "/toggle")
                        .param("assignmentId", assignmentId.toString())
                        .param("tz", "Not/ATimezone"))
                .andExpect(status().isOk());

        verify(orchestrator).toggleTaskDoneAndPlan(eq(assignmentId), eq(taskId), eq(expected));
    }

    // --- effort ---

    @Test
    void updateEffort_withValidTz_usesCorrectToday() throws Exception {
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(patch("/tasks/" + taskId + "/effort")
                        .param("assignmentId", assignmentId.toString())
                        .param("tz", tz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effortHours\":3}"))
                .andExpect(status().isOk());

        verify(orchestrator).updateTaskEffortAndPlan(eq(assignmentId), eq(taskId), eq(3), eq(expected));
    }

    @Test
    void updateEffort_withNoTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(patch("/tasks/" + taskId + "/effort")
                        .param("assignmentId", assignmentId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"effortHours\":2}"))
                .andExpect(status().isOk());

        verify(orchestrator).updateTaskEffortAndPlan(eq(assignmentId), eq(taskId), eq(2), eq(expected));
    }

    // --- title ---

    @Test
    void updateTitle_withValidTz_usesCorrectToday() throws Exception {
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(patch("/tasks/" + taskId + "/title")
                        .param("assignmentId", assignmentId.toString())
                        .param("tz", tz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isOk());

        verify(orchestrator).updateTaskTitleAndPlan(eq(assignmentId), eq(taskId), eq("New Title"), eq(expected));
    }

    @Test
    void updateTitle_withNoTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(patch("/tasks/" + taskId + "/title")
                        .param("assignmentId", assignmentId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New Title\"}"))
                .andExpect(status().isOk());

        verify(orchestrator).updateTaskTitleAndPlan(eq(assignmentId), eq(taskId), eq("New Title"), eq(expected));
    }
}