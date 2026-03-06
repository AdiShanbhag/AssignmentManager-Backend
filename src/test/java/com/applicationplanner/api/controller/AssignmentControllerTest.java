package com.applicationplanner.api.controller;

import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import com.applicationplanner.api.util.WithMockUserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AssignmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PlanningOrchestratorService orchestrator;

    @BeforeEach
    void setup() {
        WithMockUserId.set();
    }

    @AfterEach
    void teardown() {
        WithMockUserId.clear();
    }

    // --- POST /assignments ---

    @Test
    void create_withValidTz_usesCorrectToday() throws Exception {
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        Assignment stub = new Assignment();
        stub.setId(UUID.randomUUID());
        stub.setTitle("Test");
        stub.setSubject("Math");
        stub.setDueDate(expected.plusDays(7));
        when(orchestrator.createAssignmentAndPlan(any(), eq(expected))).thenReturn(stub);

        mockMvc.perform(post("/assignments")
                        .param("tz", tz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBody("Test", "Math", expected.plusDays(7).toString()))))
                .andExpect(status().isOk());

        verify(orchestrator).createAssignmentAndPlan(any(), eq(expected));
    }

    @Test
    void create_withNoTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        Assignment stub = new Assignment();
        stub.setId(UUID.randomUUID());
        stub.setTitle("Test");
        stub.setSubject("Math");
        stub.setDueDate(expected.plusDays(7));
        when(orchestrator.createAssignmentAndPlan(any(), eq(expected))).thenReturn(stub);

        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBody("Test", "Math", expected.plusDays(7).toString()))))
                .andExpect(status().isOk());

        verify(orchestrator).createAssignmentAndPlan(any(), eq(expected));
    }

    @Test
    void create_withInvalidTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        Assignment stub = new Assignment();
        stub.setId(UUID.randomUUID());
        stub.setTitle("Test");
        stub.setSubject("Math");
        stub.setDueDate(expected.plusDays(7));
        when(orchestrator.createAssignmentAndPlan(any(), eq(expected))).thenReturn(stub);

        mockMvc.perform(post("/assignments")
                        .param("tz", "Not/ATimezone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBody("Test", "Math", expected.plusDays(7).toString()))))
                .andExpect(status().isOk());

        verify(orchestrator).createAssignmentAndPlan(any(), eq(expected));
    }

    // --- PATCH /assignments/{id} ---

    @Test
    void update_withValidTz_usesCorrectToday() throws Exception {
        UUID id = UUID.randomUUID();
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(patch("/assignments/" + id)
                        .param("tz", tz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Updated\"}"))
                .andExpect(status().isOk());

        verify(orchestrator).updateAssignmentAndPlan(eq(id), any(), eq(expected));
    }

    @Test
    void update_withBlankTitle_returns400() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(patch("/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    // --- DELETE /assignments/{id} ---

    @Test
    void delete_withValidTz_usesCorrectToday() throws Exception {
        UUID id = UUID.randomUUID();
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(delete("/assignments/" + id)
                        .param("tz", tz))
                .andExpect(status().isOk());

        verify(orchestrator).removeAssignment(eq(id), eq(expected));
    }

    @Test
    void delete_withNoTz_fallsBackToUtc() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(delete("/assignments/" + id))
                .andExpect(status().isOk());

        verify(orchestrator).removeAssignment(eq(id), eq(expected));
    }

    // --- Start Date Tests ---

    /**
     * Verifies that creating an assignment with a valid start date before due date succeeds. By Claude
     */
    @Test
    void create_withValidStartDate_returns200() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate dueDate = today.plusDays(14);
        LocalDate startDate = today.plusDays(7);

        Assignment stub = new Assignment();
        stub.setId(UUID.randomUUID());
        stub.setTitle("Test");
        stub.setSubject("Math");
        stub.setDueDate(dueDate);
        stub.setStartDate(startDate);
        when(orchestrator.createAssignmentAndPlan(any(), any())).thenReturn(stub);

        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Test",
                                "subject": "Math",
                                "dueDate": "%s",
                                "startDate": "%s"
                            }
                            """.formatted(dueDate, startDate)))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that creating an assignment with startDate after dueDate returns 400. By Claude
     */
    @Test
    void create_withStartDateAfterDueDate_returns400() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate dueDate = today.plusDays(7);
        LocalDate startDate = today.plusDays(14); // start after due

        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Test",
                                "subject": "Math",
                                "dueDate": "%s",
                                "startDate": "%s"
                            }
                            """.formatted(dueDate, startDate)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that creating an assignment with startDate equal to dueDate returns 400. By Claude
     */
    @Test
    void create_withStartDateEqualToDueDate_returns400() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate dueDate = today.plusDays(7);

        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Test",
                                "subject": "Math",
                                "dueDate": "%s",
                                "startDate": "%s"
                            }
                            """.formatted(dueDate, dueDate)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifies that creating an assignment without startDate still succeeds. By Claude
     */
    @Test
    void create_withNoStartDate_returns200() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate dueDate = today.plusDays(7);

        Assignment stub = new Assignment();
        stub.setId(UUID.randomUUID());
        stub.setTitle("Test");
        stub.setSubject("Math");
        stub.setDueDate(dueDate);
        when(orchestrator.createAssignmentAndPlan(any(), any())).thenReturn(stub);

        mockMvc.perform(post("/assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "title": "Test",
                                "subject": "Math",
                                "dueDate": "%s"
                            }
                            """.formatted(dueDate)))
                .andExpect(status().isOk());
    }

    /**
     * Verifies that updating an assignment with a valid start date succeeds. By Claude
     */
    @Test
    void update_withValidStartDate_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate dueDate = today.plusDays(14);
        LocalDate startDate = today.plusDays(7);

        mockMvc.perform(patch("/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "dueDate": "%s",
                                "startDate": "%s"
                            }
                            """.formatted(dueDate, startDate)))
                .andExpect(status().isOk());

        verify(orchestrator).updateAssignmentAndPlan(eq(id), any(), any());
    }

    /**
     * Verifies that updating an assignment with startDate after dueDate returns 400. By Claude
     */
    @Test
    void update_withStartDateAfterDueDate_returns400() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDate today = LocalDate.now(ZoneId.of("UTC"));
        LocalDate dueDate = today.plusDays(7);
        LocalDate startDate = today.plusDays(14);

        mockMvc.perform(patch("/assignments/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "dueDate": "%s",
                                "startDate": "%s"
                            }
                            """.formatted(dueDate, startDate)))
                .andExpect(status().isBadRequest());
    }

    // --- Request body helper ---
    record CreateBody(String title, String subject, String dueDate) {}
}
