package com.taskflow.api.dto.response;

import com.taskflow.api.domain.WorkspaceMember;
import com.taskflow.api.domain.WorkspaceRole;
import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID id, UUID userId, String email, String fullName, WorkspaceRole role, Instant joinedAt) {

    public static WorkspaceMemberResponse of(WorkspaceMember member) {
        return new WorkspaceMemberResponse(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getEmail(),
                member.getUser().getFullName(),
                member.getRole(),
                member.getCreatedAt());
    }
}
