package com.applicationplanner.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PlanControllerTodayParamTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void planEndpointAcceptsTodayParameter() throws Exception {
        mockMvc.perform(get("/plan")
                        .param("today", "2026-02-09"))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
