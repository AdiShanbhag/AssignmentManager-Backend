package com.applicationplanner.api.config;

import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;


public class SpeedRunner implements CommandLineRunner {

    private final PlanningOrchestratorService orchestrator;

    public SpeedRunner(PlanningOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @Override
    public void run(String... args) {
        LocalDate today = LocalDate.now(); // later: accept from client

        Assignment a = new Assignment();
        a.setTitle("Test A");
        a.setSubject("Sub A");
        a.setDueDate(today.plusDays(7));
        a.setCreatedAt(Instant.now());
        a.setPlanningDays(1);

        orchestrator.createAssignmentAndPlan(a, today);

        System.out.println("Seed done");
    }
}