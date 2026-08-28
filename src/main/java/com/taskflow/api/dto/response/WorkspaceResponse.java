package com.taskflow.api.dto.response;

import com.taskflow.api.domain.Workspace;
import com.taskflow.api.domain.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id, String name, String slug, UUID ownerId, WorkspaceRole callerRole, Instant createdAt) {

    public static WorkspaceResponse of(Workspace workspace, WorkspaceRole callerRole) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getOwner().getId(),
                callerRole,
                workspace.getCreatedAt());
    }
}
