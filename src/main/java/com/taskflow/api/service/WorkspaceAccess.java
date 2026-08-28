package com.taskflow.api.service;

import com.taskflow.api.domain.Workspace;
import com.taskflow.api.domain.WorkspaceMember;
import com.taskflow.api.domain.WorkspaceRole;
import com.taskflow.api.exception.ForbiddenOperationException;
import com.taskflow.api.exception.ResourceNotFoundException;
import com.taskflow.api.repository.WorkspaceMemberRepository;
import com.taskflow.api.repository.WorkspaceRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Central place for "can this user act on this workspace" checks. Every workspace-scoped
 * operation runs through here so the rules live in one file instead of being sprinkled
 * across services.
 *
 * <p>When the caller isn't a member at all we answer with {@link ResourceNotFoundException}
 * rather than a 403: a stranger shouldn't be able to tell whether a given workspace id exists.
 */
@Component
public class WorkspaceAccess {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;

    public WorkspaceAccess(WorkspaceRepository workspaceRepository, WorkspaceMemberRepository memberRepository) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
    }

    public WorkspaceMember require(UUID workspaceId, UUID userId, WorkspaceRole minimumRole) {
        WorkspaceMember membership = memberRepository
                .findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Workspace", workspaceId));

        if (!membership.getRole().atLeast(minimumRole)) {
            throw new ForbiddenOperationException(
                    "This action requires the %s role or higher in the workspace".formatted(minimumRole));
        }
        return membership;
    }

    public Workspace requireWorkspace(UUID workspaceId, UUID userId, WorkspaceRole minimumRole) {
        require(workspaceId, userId, minimumRole);
        return workspaceRepository
                .findById(workspaceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Workspace", workspaceId));
    }
}
