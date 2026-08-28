package com.taskflow.api.dto.request;

import com.taskflow.api.domain.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull WorkspaceRole role) {
}
