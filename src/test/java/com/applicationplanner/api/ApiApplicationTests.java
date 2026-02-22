package com.applicationplanner.api;

import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Task;
import com.applicationplanner.api.record.AssignmentPlanView;
import com.applicationplanner.api.service.impl.PlanningOrchestratorServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@ActiveProfiles("test")
class ApiApplicationTests {

	@Test
	void contextLoads() {
	}
}
