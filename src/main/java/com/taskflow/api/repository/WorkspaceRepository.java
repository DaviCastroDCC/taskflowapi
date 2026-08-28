package com.taskflow.api.repository;

import com.taskflow.api.domain.Workspace;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {

    Optional<Workspace> findBySlug(String slug);

    boolean existsBySlug(String slug);

    @Query("select distinct w from Workspace w join w.members m where m.user.id = :userId")
    List<Workspace> findAllByMemberUserId(@Param("userId") UUID userId);
}
