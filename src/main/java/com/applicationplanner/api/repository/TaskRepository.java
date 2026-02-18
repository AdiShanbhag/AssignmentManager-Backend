package com.applicationplanner.api.repository;

import com.applicationplanner.api.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {
    List<Task> findByAssignmentId(UUID assignmentId);
    List<Task> findAllByAssignmentIdIn(List<UUID> assignmentIds);
}
