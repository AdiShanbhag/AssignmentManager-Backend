package com.applicationplanner.api.dto.responseDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@Getter
@Setter
public class AssignmentResponse {
    public UUID id;
    public String title;
    public String subject;
    public LocalDate dueDate;

    public String status;            // "ON_TRACK" / "AT_RISK" / "SCREWED" (optional now)
    public List<TaskResponse> tasks; // optional for list endpoint, required for detail

    public AssignmentResponse() {

    }
}
