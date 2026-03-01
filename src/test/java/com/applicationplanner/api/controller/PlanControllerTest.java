package com.applicationplanner.api.controller;

import com.applicationplanner.api.service.PlanningOrchestratorService;
import com.applicationplanner.api.util.WithMockUserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanningOrchestratorService orchestrator;

    @BeforeEach
    void setup() {
        WithMockUserId.set();
        // stub default return so controller mapping doesn't blow up
        when(orchestrator.getPlanView(any())).thenReturn(List.of());
    }

    @AfterEach
    void teardown() {
        WithMockUserId.clear();
    }

    @Test
    void getPlan_withValidTz_usesCorrectToday() throws Exception {
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(get("/plan").param("tz", tz))
                .andExpect(status().isOk());

        verify(orchestrator).getPlanView(eq(expected));
    }

    @Test
    void getPlan_withNoTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(get("/plan"))
                .andExpect(status().isOk());

        verify(orchestrator).getPlanView(eq(expected));
    }

    @Test
    void getPlan_withInvalidTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(get("/plan").param("tz", "Bad/Zone"))
                .andExpect(status().isOk());

        verify(orchestrator).getPlanView(eq(expected));
    }
}