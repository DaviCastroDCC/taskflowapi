package com.taskflow.api.repository;

import com.taskflow.api.domain.Project;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, UUID> {

    List<Project> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    boolean existsByWorkspaceIdAndKeyIgnoreCase(UUID workspaceId, String key);

    /**
     * Locks the row for the duration of the transaction so concurrent task creation under the
     * same project can't read the same {@code taskSequence} twice and hand out duplicate codes.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id")
    Optional<Project> findByIdForUpdate(@Param("id") UUID id);
}
