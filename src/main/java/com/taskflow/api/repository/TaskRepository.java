package com.taskflow.api.repository;

import com.taskflow.api.domain.Task;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    boolean existsByProjectIdAndTaskNumber(UUID projectId, long taskNumber);

    /** Loads a task together with its project and workspace so access checks don't trigger extra queries. */
    @Query("select t from Task t join fetch t.project p join fetch p.workspace where t.id = :id")
    Optional<Task> findDetailedById(@Param("id") UUID id);
}
