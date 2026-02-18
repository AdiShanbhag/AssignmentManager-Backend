package com.applicationplanner.api.mapper;

import com.applicationplanner.api.dto.responseDTO.AssignmentResponse;
import com.applicationplanner.api.dto.responseDTO.TaskResponse;
import com.applicationplanner.api.model.Assignment;
import com.applicationplanner.api.model.Task;

import java.util.Comparator;
import java.util.List;

public class AssignmentMapper {
    public static AssignmentResponse toResponse(Assignment a, List<Task> tasks) {
        AssignmentResponse res = new AssignmentResponse();
        res.id = a.getId();
        res.title = a.getTitle();
        res.subject = a.getSubject();
        res.dueDate = a.getDueDate();

        res.tasks = tasks.stream()
                .sorted(Comparator.comparingInt(Task::getOrderIndex))
                .map(AssignmentMapper::toResponse)
                .toList();

        // optional for later:
        // res.status = ...
        return res;
    }

    public static TaskResponse toResponse(Task t) {
        TaskResponse res = new TaskResponse();
        res.id = t.getId();
        res.title = t.getTitle();
        res.isDone = t.isDone();
        res.effortHours = t.getEffortHours();
        res.orderIndex = t.getOrderIndex();
        res.targetDate = t.getTargetDate(); // can be null
        res.isUnscheduled = t.isUnscheduled();
        return res;
    }
}
