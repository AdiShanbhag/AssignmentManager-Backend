package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.AvailabilityRequest;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    private final PlanningOrchestratorService orchestrator;

    public AvailabilityController(PlanningOrchestratorService orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public Availability getAvailability() {
        return orchestrator.getAvailabilityOrDefault();
    }

    @PutMapping
    public void setAvailability(@Valid @RequestBody AvailabilityRequest req, @RequestParam(required = false) String today) {
        LocalDate effectiveToday =
                (today != null) ? LocalDate.parse(today) : LocalDate.now();

        Availability a = new Availability();
        a.setMonHours(req.monHours());
        a.setTueHours(req.tueHours());
        a.setWedHours(req.wedHours());
        a.setThuHours(req.thuHours());
        a.setFriHours(req.friHours());
        a.setSatHours(req.satHours());
        a.setSunHours(req.sunHours());

        orchestrator.setAvailabilityAndPlan(a, effectiveToday);
    }
}