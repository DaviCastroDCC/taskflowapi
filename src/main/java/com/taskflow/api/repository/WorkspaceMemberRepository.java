package com.taskflow.api.repository;

import com.taskflow.api.domain.WorkspaceMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {

    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

    long countByWorkspaceId(UUID workspaceId);

    @Query("select m from WorkspaceMember m "
            + "join fetch m.workspace w join fetch w.owner "
            + "where m.user.id = :userId order by m.createdAt")
    List<WorkspaceMember> findAllForUser(@Param("userId") UUID userId);

    @Query("select m from WorkspaceMember m join fetch m.user "
            + "where m.workspace.id = :workspaceId order by m.createdAt")
    List<WorkspaceMember> findAllByWorkspace(@Param("workspaceId") UUID workspaceId);
}
