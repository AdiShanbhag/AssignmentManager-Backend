package com.applicationplanner.api.controller;

import com.applicationplanner.api.dto.requestDTO.AvailabilityRequest;
import com.applicationplanner.api.model.Availability;
import com.applicationplanner.api.service.PlanningOrchestratorService;
import com.applicationplanner.api.util.TimezoneResolver;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/availability")
public class AvailabilityController {

    private final PlanningOrchestratorService orchestrator;
    private final TimezoneResolver timezoneResolver;

    public AvailabilityController(PlanningOrchestratorService orchestrator,
                                  TimezoneResolver timezoneResolver)
    {
        this.orchestrator = orchestrator;
        this.timezoneResolver = timezoneResolver;
    }

    @GetMapping
    public Availability getAvailability() {
        return orchestrator.getAvailabilityOrDefault();
    }

    @PutMapping
    public void setAvailability(@Valid @RequestBody AvailabilityRequest req) {
        LocalDate today = timezoneResolver.resolveToday();

        Availability a = new Availability();
        a.setMonHours(req.monHours());
        a.setTueHours(req.tueHours());
        a.setWedHours(req.wedHours());
        a.setThuHours(req.thuHours());
        a.setFriHours(req.friHours());
        a.setSatHours(req.satHours());
        a.setSunHours(req.sunHours());

        orchestrator.setAvailabilityAndPlan(a, today);
    }
}