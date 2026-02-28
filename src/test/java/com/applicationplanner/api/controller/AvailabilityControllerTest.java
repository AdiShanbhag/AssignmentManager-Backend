package com.applicationplanner.api.controller;

import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.service.PlanningOrchestratorService;
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

import java.time.LocalDate;
import java.time.ZoneId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AvailabilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlanningOrchestratorService orchestrator;

    @BeforeEach
    void setup() {
        WithMockUserId.set();
        when(orchestrator.getAvailabilityOrDefault()).thenReturn(new Availability());
    }

    @AfterEach
    void teardown() {
        WithMockUserId.clear();
    }

    // --- GET /availability ---

    @Test
    void getAvailability_returns200() throws Exception {
        mockMvc.perform(get("/availability"))
                .andExpect(status().isOk());

        verify(orchestrator).getAvailabilityOrDefault();
    }

    // --- PUT /availability ---

    @Test
    void setAvailability_withValidTz_usesCorrectToday() throws Exception {
        String tz = "Australia/Sydney";
        LocalDate expected = LocalDate.now(ZoneId.of(tz));

        mockMvc.perform(put("/availability")
                        .param("tz", tz)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monHours\":2,\"tueHours\":2,\"wedHours\":2,\"thuHours\":2,\"friHours\":2,\"satHours\":4,\"sunHours\":2}"))
                .andExpect(status().isOk());

        verify(orchestrator).setAvailabilityAndPlan(any(), eq(expected));
    }

    @Test
    void setAvailability_withNoTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(put("/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monHours\":2,\"tueHours\":2,\"wedHours\":2,\"thuHours\":2,\"friHours\":2,\"satHours\":4,\"sunHours\":2}"))
                .andExpect(status().isOk());

        verify(orchestrator).setAvailabilityAndPlan(any(), eq(expected));
    }

    @Test
    void setAvailability_withInvalidTz_fallsBackToUtc() throws Exception {
        LocalDate expected = LocalDate.now(ZoneId.of("UTC"));

        mockMvc.perform(put("/availability")
                        .param("tz", "Invalid/Zone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"monHours\":2,\"tueHours\":2,\"wedHours\":2,\"thuHours\":2,\"friHours\":2,\"satHours\":4,\"sunHours\":2}"))
                .andExpect(status().isOk());

        verify(orchestrator).setAvailabilityAndPlan(any(), eq(expected));
    }
}