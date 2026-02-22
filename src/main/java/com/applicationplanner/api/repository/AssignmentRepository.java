package com.applicationplanner.api.repository;

import com.applicationplanner.api.model.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID>
{
    List<Assignment> findAllByUserIdOrderByDueDateAsc(UUID userId);

    Optional<Assignment> findByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
