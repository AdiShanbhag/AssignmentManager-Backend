package com.applicationplanner.api.controller;

import com.applicationplanner.api.service.auth.GoogleTokenVerifier;
import com.applicationplanner.api.record.GoogleUserPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GoogleAuthControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    GoogleTokenVerifier googleTokenVerifier;

    @Test
    void authGoogle_createsUserAndReturnsJwt() throws Exception {
        when(googleTokenVerifier.verify(anyString()))
                .thenReturn(new GoogleUserPayload("sub-123", "t@e.com", "Test User"));

        mvc.perform(
                        post("/auth/google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"idToken\":\"fake\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(jsonPath("$.email").value("t@e.com"))
                .andExpect(jsonPath("$.displayName").value("Test User"));
    }
}